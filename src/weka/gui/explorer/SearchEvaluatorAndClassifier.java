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
    private final Instances inst;
    private final int testMode;
    private final int numFolds;
    private final int seed; 
    private final int classIndex;
    private final double percent;
    private final ASEvaluation evaluator;
    private final ASSearch search;
    private final Classifier classifier;
    private final JProgressBar progressBar;
    private final int percentThreads;
    private final boolean selectedPreserveOrder;

    public SearchEvaluatorAndClassifier(Logger m_Log, Instances inst, int testMode, int numFolds, int seed, int classIndex, 
            double percent, ASEvaluation evaluator, ASSearch search, Classifier classifier, 
            JProgressBar progressBar, int pThreads, boolean selectedPreserveOrder) throws Exception {
        this.log = m_Log;
        this.inst = new Instances(inst);
        this.testMode = testMode;
        this.numFolds = numFolds;
        this.seed = seed;
        this.classIndex = classIndex;
        this.percent = percent;
        
        if(evaluator != null && search != null){
            this.evaluator = ASEvaluation.makeCopies(evaluator, 1)[0];
            this.search = ASSearch.makeCopies(search, 1)[0];
        }else{
            this.evaluator = null;
            this.search = null;
        }
        
        this.classifier = AbstractClassifier.makeCopy(classifier);
        this.progressBar = progressBar;
        this.percentThreads = pThreads;
        this.selectedPreserveOrder = selectedPreserveOrder;
    }
    
    public ResultsAttrSelExp run() {
        Random random;
        Instances train, newTrain, test, newTest = null;
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
        
        try{
            switch (testMode) {
                case 0: // Hold-out mode
                    if(!selectedPreserveOrder){
                        random = new Random(seed);
                        inst.randomize(random);
                    }
                    
                    int trainSize = (int) Math.round(inst.numInstances() * percent / 100);
                    int testSize = inst.numInstances() - trainSize;
                    
                    //build train and test
                    train = new Instances(inst, 0, trainSize);
                    test = new Instances(inst, trainSize, testSize);

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
                    break;
                case 1: case 2: // CV and Leave One Out mode
                    random = new Random(seed);
                    inst.randomize(random);

                    //setup meta-classifier
                    Classifier classif;
                    
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
                    eval.crossValidateModel(classif, inst, numFolds, new Random(seed));
                    newTest = new Instances(inst);
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
        
        ResultsAttrSelExp result = new ResultsAttrSelExp(eval, newTest, inst, evaluator, search, classifier, numAttr);
        
        synchronized(progressBar) {
            progressBar.setValue(progressBar.getValue()+percentThreads);
        }
        
        return result;
    }

    @Override
    public ResultsAttrSelExp call() throws Exception {
        ResultsAttrSelExp res = this.run();
        
        return res;
    }
}
