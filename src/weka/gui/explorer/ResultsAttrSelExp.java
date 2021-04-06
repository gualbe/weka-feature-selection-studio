/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.gui.explorer;


import java.util.List;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;

/**
 *
 * @author Andrés Manuel
 */
public class ResultsAttrSelExp {
    private Evaluation evalClassifier;
    private Instances test;
    private Instances inst;
    private final ASEvaluation evaluator;
    private final ASSearch search;
    private final Classifier classifier;
    private final int numAttr;

    public ResultsAttrSelExp(Evaluation evalClassifier, Instances test, Instances inst, ASEvaluation evaluator, ASSearch search, Classifier classifier, int numAttr) {
        this.evalClassifier = evalClassifier;
        this.test = test;
        this.inst = inst;
        this.evaluator = evaluator;
        this.search = search;
        this.classifier = classifier;
        this.numAttr = numAttr;
    }

    public Evaluation getEvalClassifier() {
        return evalClassifier;
    }

    public Instances getTest() {
        return test;
    }
    
    public Instances getInst() {
        return inst;
    }

    public ASEvaluation getEvaluator() {
        return evaluator;
    }

    public ASSearch getSearch() {
        return search;
    }

    public Classifier getClassifier() {
        return classifier;
    }
    
    public int getNumAttr(){
        return numAttr;
    }
}
