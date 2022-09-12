/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.gui.explorer;

import java.util.Random;
import java.util.concurrent.Callable;
import javax.swing.JProgressBar;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.ASEvaluation;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.gui.Logger;
import weka.gui.SysErrLog;

/**
 *
 * @author Andrés Manuel
 */
public class SearchEvaluatorAndClassifier implements Callable<ResultsAttrSelExp>{
    private Logger log = new SysErrLog();
    private Instances inst;
    private int testMode;
    private int seed; 
    private int classIndex;
    private ASEvaluation evaluator;
    private ASSearch search;
    private Classifier classifier;
    private final JProgressBar progressBar;
    private static int percentThreads;
    private Instances train;
    private Instances test;
    private final int fold;
    private int numTareas;

    public SearchEvaluatorAndClassifier(Logger m_Log, Instances inst, int testMode, int seed, int classIndex, 
            ASEvaluation evaluator, ASSearch search, Classifier classifier, 
            JProgressBar progressBar, int numTareas, Instances train, Instances test, int fold) throws Exception {
        this.log = m_Log;
        this.inst = new Instances(inst);
        this.testMode = testMode;
        this.seed = seed;
        this.classIndex = classIndex;
        
        if(train == null){
            this.train = train;
        }else{
            this.train = new Instances(train);
        }
        
        if(test == null){
            this.test = test;
        }else{
            this.test = new Instances(test);
        }
        
        
        if(evaluator != null && search != null){
            this.evaluator = ASEvaluation.makeCopies(evaluator, 1)[0];
            this.search = ASSearch.makeCopies(search, 1)[0];
        }else{
            this.evaluator = null;
            this.search = null;
        }
        
        this.classifier = AbstractClassifier.makeCopy(classifier);
        this.progressBar = progressBar;
        this.fold = fold;
        this.numTareas = numTareas;
        percentThreads = 0;
    }
    
    public ResultsAttrSelExp run() {
        Instances newTrain, newTest = null;
        AttributeSelection filter;
        //Classifier cls = classifier;
        Classifier cls = null;
        try {
            cls = AbstractClassifier.makeCopy(classifier);
        } catch (Exception ex) {
            log.logMessage(ex.getMessage());
            log.statusMessage("See error log");
        }
        //Inside eval all the metrics and errors are generated
        Evaluation eval = null;
        int numAttr = 0;
        ResultsAttrSelExp result = null;
        //setup meta-classifier
        Classifier classif;
        
        try{
            switch (testMode) {
                case 0: // Hold-out mode
                    //build filter for AttributeSelection
                    filter = new AttributeSelection();
                    
                    if(evaluator != null && search != null){
                        filter.setEvaluator(evaluator);
                        filter.setSearch(search);
                        filter.setInputFormat(train);
                        newTrain = Filter.useFilter(train, filter);
                        newTest = Filter.useFilter(test, filter);
                    }else{ //no attribute selection
                        newTrain = train;
                        newTest = test;
                    }

                    //Classifier
                    cls.buildClassifier(newTrain);
                    
                    eval = new Evaluation(newTrain);
                    eval.evaluateModel(cls, newTest);
                    numAttr = newTest.numAttributes();
                    
                    train = null;
                    test = null;
                    newTrain = null;
                    newTest = null;
                    
                    System.gc();
                    
                    result = new ResultsAttrSelExp(eval, newTest, inst, evaluator, search, classifier, numAttr, 0);
                    break;
                case 1:  // CV mode
                    if(evaluator != null && search != null){
                        classif = new AttributeSelectedClassifier();
                        ((AttributeSelectedClassifier)classif).setClassifier(cls);
                        ((AttributeSelectedClassifier)classif).setEvaluator(evaluator);
                        ((AttributeSelectedClassifier)classif).setSearch(search);
                        
                        //number of attributes
                        filter = new AttributeSelection();
                        filter.setEvaluator(evaluator);
                        filter.setSearch(search);
                        filter.setInputFormat(inst);
                        Instances attr = Filter.useFilter(inst, filter);
                        numAttr = attr.numAttributes();
                    } else { //no attribute selection
                        classif = cls;
                        
                        numAttr = inst.numAttributes();
                    }
                    
                    //Classifier
                    classif.buildClassifier(train);

                    eval = new Evaluation(train);
                    eval.evaluateModel(classif, test);
                    result = new ResultsAttrSelExp(eval, test, inst, evaluator, search, classifier, numAttr, fold);
                    break;
                case 2:  // Leave One Out mode
                    if(evaluator != null && search != null){
                        classif = new AttributeSelectedClassifier();
                        ((AttributeSelectedClassifier)classif).setClassifier(cls);
                        ((AttributeSelectedClassifier)classif).setEvaluator(evaluator);
                        ((AttributeSelectedClassifier)classif).setSearch(search);
                        
                        //number of attributes
                        filter = new AttributeSelection();
                        filter.setEvaluator(evaluator);
                        filter.setSearch(search);
                        filter.setInputFormat(inst);
                        Instances attr = Filter.useFilter(inst, filter);
                        numAttr = attr.numAttributes();
                    } else { //no attribute selection
                        classif = cls;
                        
                        numAttr = inst.numAttributes();
                    }
                    
                    //cross-validate classifier
                    eval = new Evaluation(inst);
                    eval.crossValidateModel(classif, inst, inst.numInstances(), new Random(seed));
                    result = new ResultsAttrSelExp(eval, test, inst, evaluator, search, classifier, numAttr, fold);
                    break;
                default:
                    throw new Exception("Test mode not implemented");
            }
        }catch (Exception ex) {
            log.logMessage("With " + inst.relationName() + ", " + evaluator.getClass().getSimpleName() + ", " +
                search.getClass().getSimpleName() + " and " + classifier.getClass().getSimpleName() + 
                " throw the exception: " + ex.getMessage());
            log.statusMessage("See error log");
        } 
        
        synchronized(progressBar) {
            //progressBar.setValue(progressBar.getValue()+percentThreads);
            percentThreads++;
            progressBar.setValue((100*percentThreads)/numTareas);
        }
        
        this.inst = null;
        
        System.gc();
        
        return result;
    }

    @Override
    public ResultsAttrSelExp call() throws Exception {
        ResultsAttrSelExp res = this.run();
        
        return res;
    }

    public void setNumTareas(int numTareas) {
        this.numTareas = numTareas;
    }
}
