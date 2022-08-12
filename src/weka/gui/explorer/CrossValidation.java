package weka.gui.explorer;

import weka.core.Instances;

import java.util.Iterator;
import java.util.Random;

public class CrossValidation implements Iterator<Instances []> {

    protected Instances instances;
    protected Instances [] result;
    protected int currentFold, numFolds;
    protected long seed = 1;

    public CrossValidation(Instances instances, int numFolds) {
        this.instances = new Instances(instances);
        this.instances.randomize(new Random(seed));
        if (numFolds == -1) // leave-one-out
            this.numFolds = instances.numInstances();
        else
            this.numFolds = numFolds;
        result = new Instances[2];
        currentFold = 0;
    }

    @Override
    public boolean hasNext() {
        return currentFold < numFolds;
    }

    @Override
    public Instances [] next() {
        result[0] = instances.trainCV(numFolds, currentFold);
        result[1] = instances.testCV(numFolds, currentFold);
        currentFold++;
        return result;
    }

}
