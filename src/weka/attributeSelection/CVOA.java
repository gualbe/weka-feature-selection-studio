package weka.attributeSelection;

import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JOptionPane;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Range;
import weka.core.Utils;

/**
 *
 * @author Andrés Manuel
 */
public class CVOA extends ASSearch implements OptionHandler, StartSetHandler {

    /**
     * holds the start set for the search as a Range
     */
    protected Range m_startRange;

    /**
     * holds an array of starting attributes
     */
    protected int[] m_starting;

    /**
     * holds the class index
     */
    protected int m_classIndex;
    
    /** does the data have a class */
    protected boolean m_hasClass;

    /**
     * number of attributes in the data
     */
    protected int m_numAttribs;

    /**
     * holds the merit of the best subset found
     */
    protected double m_bestMerit;

    /**
     * for debugging
     */
    protected boolean m_debug;
    
    //Options CVOA
    protected int iterations;
    protected int numStrains;
    protected String seeds;
    protected String minSpread;
    protected String maxSpread;
    protected String minSuperspread;
    protected String maxSuperspread;
    protected String socialDistancing; // Iterations without social distancing
    protected String pIsolation;
    protected String pTravel;
    protected String pReinfection;
    protected String superspreaderPerc;
    protected String deathPerc;
    
    /**
     * Returns a string describing this search method
     *
     * @return a description of the search method suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "CVOA:\n\n"
                + "A novel bioinspired metaheuristic, simulating how the coronavirus spreads "
                + "and infects healthy people. From an initial individual (thepatient zero), "
                + "the coronavirus infects new patients at known rates, creating new "
                + "populations of infected people. Every individual can either die or "
                + "infect and, afterwards, be sent to the recovered population. Relevant "
                + "terms such as re-infection probability, super-spreading rate or traveling "
                + "rate are introduced in the model in order to simulate as accurately "
                +  "as possible the coronavirus activity.\n";
    }

    /**
     * Constructor
     */
    public CVOA() {
        resetOptions();
    }

    @Override
    public void setStartSet(String starSet) throws Exception {
        m_startRange.setRanges(starSet);
    }

    @Override
    public String getStartSet() {
        return m_startRange.getRanges();
    }

    @Override
    public int[] search(ASEvaluation ASEval, Instances data) throws Exception {
        double best_merit;
        m_numAttribs = data.numAttributes();
        BitSet best_group = new BitSet(m_numAttribs);

        if (!(ASEval instanceof SubsetEvaluator)) {
            throw new Exception(ASEval.getClass().getName() + " is not a "
                    + "Subset evaluator!");
        }

        m_hasClass = true;
        m_classIndex = data.classIndex();
        m_startRange.setUpper(m_numAttribs - 1);

        if (!(getStartSet().equals(""))) {
            m_starting = m_startRange.getSelection();
        }
        
        SubsetEvaluator ASEvaluator = (SubsetEvaluator) ASEval;

        //set the options for the algorithms
        int[] seedsInt = stringToArrayInt(seeds, "seeds");
        
        if(seedsInt == null){
            seedsInt = new int[numStrains];
            defaultValuesInt(seedsInt, 1);
        }
        
        int[] minSpreadInt = stringToArrayInt(minSpread, "minSpread");
        
        if(minSpreadInt == null){
            minSpreadInt = new int[numStrains];
            defaultValuesInt(minSpreadInt, 0);
        }
        
        int[] maxSpreadInt = stringToArrayInt(maxSpread, "maxSpread");
        
        if(maxSpreadInt == null){
            maxSpreadInt = new int[numStrains];
            defaultValuesInt(maxSpreadInt, 5);
        }
        
        int[] minSuperSpreadInt = stringToArrayInt(minSuperspread, "minSuperspread");
        
        if(minSuperSpreadInt == null){
           minSuperSpreadInt = new int[numStrains];
           defaultValuesInt(minSuperSpreadInt, 6);
        }
        
        int[] maxSuperSpreadInt = stringToArrayInt(maxSuperspread, "maxSuperspread");
        
        if(maxSuperSpreadInt == null){
            maxSuperSpreadInt = new int[numStrains];
            defaultValuesInt(maxSuperSpreadInt, 15);
        }
        
        int[] socialDistancingInt = stringToArrayInt(socialDistancing, "socialDistancing");
        
        if(socialDistancingInt == null){
            socialDistancingInt = new int[numStrains];
            defaultValuesInt(socialDistancingInt, 3);
        }
        
        double[] pIsolationDouble = stringToArrayDouble(pIsolation, "pIsolation");
        
        if(pIsolationDouble == null){
            pIsolationDouble = new double[numStrains];
            defaultValuesDouble(pIsolationDouble, 0.7);
        }
        
        double[] pTravelDouble = stringToArrayDouble(pTravel, "pTravel");
        
        if(pTravelDouble == null){
            pTravelDouble = new double[numStrains];
            defaultValuesDouble(pTravelDouble, 0.1);
        }
        
        double[] pReinfectionDouble = stringToArrayDouble(pReinfection, "pReinfection");
        
        if(pReinfectionDouble == null){
            pReinfectionDouble = new double[numStrains];
            defaultValuesDouble(pReinfectionDouble, 0.02);
        }
        
        double[] superspreaderPercDouble = stringToArrayDouble(superspreaderPerc, "superspreaderPerc");
        
        if(superspreaderPercDouble == null){
            superspreaderPercDouble = new double[numStrains];
            defaultValuesDouble(superspreaderPercDouble, 0.1);
        }
        
        double[] deathPercDouble = stringToArrayDouble(deathPerc, "deathPerc");
        
        if(deathPercDouble == null){
            deathPercDouble = new double[numStrains];
            defaultValuesDouble(deathPercDouble, 0.05);
        }
        
        //If an initial subset of attributes is entered, pass it to be patient zero
        if (m_starting != null) {
            CVOASearch.initializePandemic(new Individual(), ASEvaluator, m_debug, m_starting);
        }else{
            CVOASearch.initializePandemic(new Individual(), ASEvaluator, m_debug, null);
        }
        
        ExecutorService pool = Executors.newFixedThreadPool(numStrains);

        Collection<CVOASearch> concurrentCVOAs = new LinkedList<>();

        for(int i = 0; i < numStrains; i++){
            concurrentCVOAs.add(new CVOASearch(m_numAttribs-1, iterations, ("Strain #"+(i+1)), seedsInt[i], minSpreadInt[i], 
            maxSpreadInt[i], minSuperSpreadInt[i], maxSuperSpreadInt[i], pTravelDouble[i], pReinfectionDouble[i],
            superspreaderPercDouble[i], deathPercDouble[i], pIsolationDouble[i], socialDistancingInt[i]));
        }
        
        List<Future<Individual>> results = new LinkedList<Future<Individual>>();

        results = pool.invokeAll(concurrentCVOAs);

        pool.shutdown();

        best_group = CVOASearch.transformador(CVOASearch.bestSolution.getData());

        //Evaluate the initial subset
        best_merit = ASEvaluator.evaluateSubset(best_group);

        m_bestMerit = best_merit;
        return attributeList(best_group);
    }

    /**
     * converts a BitSet into a list of attribute indexes
     *
     * @param group the BitSet to convert
     * @return an array of attribute indexes
     *
     */
    protected int[] attributeList(BitSet group) {
        int count = 0;

        // count how many were selected
        for (int i = 0; i < m_numAttribs; i++) {
            if (group.get(i)) {
                count++;
            }
        }

        int[] list = new int[count];
        count = 0;

        for (int i = 0; i < m_numAttribs; i++) {
            if (group.get(i)) {
                list[count++] = i;
            }
        }

        return list;
    }

    public void setOptions(String[] options) throws Exception {
        String optionString;
        resetOptions();

        optionString = Utils.getOption('P', options);
        if (optionString.length() != 0) {
            setStartSet(optionString);
        }

        optionString = Utils.getOption('I', options);

        if (optionString.length() != 0) {
            setIterations(Integer.parseInt(optionString));
        }
        
        optionString = Utils.getOption('C', options);

        if (optionString.length() != 0) {
            setNumStrains(Integer.parseInt(optionString));
        }
        
        optionString = Utils.getOption('S', options);
        
        if (optionString.length() != 0) {
            setSeeds(optionString);
        }

        optionString = Utils.getOption('A', options);

        if (optionString.length() != 0) {
            setMinSpread(optionString);
        }
        
        optionString = Utils.getOption('B', options);

        if (optionString.length() != 0) {
            setMaxSpread(optionString);
        }
        
        optionString = Utils.getOption('D', options);

        if (optionString.length() != 0) {
            setMinSuperspread(optionString);
        }
        
        optionString = Utils.getOption('E', options);

        if (optionString.length() != 0) {
            setMaxSuperspread(optionString);
        }
        
        optionString = Utils.getOption('F', options);

        if (optionString.length() != 0) {
            setSocialDistancing(optionString);
        }
        
        optionString = Utils.getOption('G', options);

        if (optionString.length() != 0) {
            setpIsolation(optionString);
        }
        
        optionString = Utils.getOption('H', options);

        if (optionString.length() != 0) {
            setpTravel(optionString);
        }
        
        optionString = Utils.getOption('J', options);

        if (optionString.length() != 0) {
            setpReinfection(optionString);
        }
        
        optionString = Utils.getOption('K', options);

        if (optionString.length() != 0) {
            setSuperspreaderPerc(optionString);
        }
        
        optionString = Utils.getOption('M', options);

        if (optionString.length() != 0) {
            setDeathPerc(optionString);
        }

        m_debug = Utils.getFlag('Z', options);
    }

    public String[] getOptions() {
        Vector<String> options = new Vector<String>();

        if (!(getStartSet().equals(""))) {
            options.add("-P");
            options.add("" + startSetToString());
        }

        options.add("-I");
        options.add("" + iterations);
        
        options.add("-C");
        options.add("" + numStrains);
        
        options.add("-S");
        options.add("" + seeds);

        options.add("-A");
        options.add("" + minSpread);

        options.add("-B");
        options.add("" + maxSpread);
        
        options.add("-D");
        options.add("" + minSuperspread);

        options.add("-E");
        options.add("" + maxSuperspread);
        
        options.add("-F");
        options.add("" + socialDistancing);
        
        options.add("-G");
        options.add("" + pIsolation);
        
        options.add("-H");
        options.add("" + pTravel);
        
        options.add("-J");
        options.add("" + pReinfection);
        
        options.add("-K");
        options.add("" + superspreaderPerc);
        
        options.add("-M");
        options.add("" + deathPerc);

        return options.toArray(new String[0]);
    }

    protected void resetOptions() {
        m_starting = null;
        m_startRange = new Range();
        m_classIndex = -1;
        m_debug = false;
        iterations = 30;
        numStrains = 1;
        seeds = "1";
        minSpread = "0";
        maxSpread = "5";
        minSuperspread = "6";
        maxSuperspread = "15";
        socialDistancing = "3";
        pIsolation = "0.7";
        pTravel = "0.1";
        pReinfection = "0.02";
        superspreaderPerc = "0.1";
        deathPerc = "0.05";
    }

    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector<Option>(14);

        newVector.addElement(new Option("\tSpecify a starting set of attributes."
                + "\n\tEg. 1,3,5-7.", "P", 1, "-P <start set>"));
        newVector.addElement(new Option("\tMaximum number of iterations"
                + "\n\tconsidered for the algorithm to converge.", "I", 12, "-I <num>"));
        newVector.addElement(new Option("\tNumber of strains"
                + "\n\tconsidered so that there is diversity of solutions.", "C", 3, "-C <num>"));
        newVector.addElement(new Option("\tInitialize the seeds of each strain"
                + "\n\tconsidered in the execution.", "S", 1, "-S <string>"));
        newVector.addElement(new Option("\tMinimum number of people infected by an individual"
                + "\n\twhen propagation occurs without being super-propagating.", "A", 1, "-A <string>"));
        newVector.addElement(new Option("\tMaximum number of people infected by an individual"
                + "\n\twhen propagation occurs without being super-propagating.", "B", 1, "-B <string>"));
        newVector.addElement(new Option("\tMinimum number of people infected by an individual"
                + "\n\twhen propagation occurs being super-propagating.", "D", 1, "D <string>"));
        newVector.addElement(new Option("\tMaximum number of people infected by an individual"
                + "\n\twhen propagation occurs being super-propagating.", "E", 1, "-E <string>"));
        newVector.addElement(new Option("\tDistance between individuals between whom"
                + "\n\tproduces propagation.", "F", 1, "-F <string>"));
        newVector.addElement(new Option("\tIsolation probability"
                + "\n\tbetween individuals whether they are infected or not.", "G", 1, "-G <string>"));
        newVector.addElement(new Option("\tProbability that individuals perform"
                + "\n\ttrips are infected or not.", "H", 1, "-H <string>"));
        newVector.addElement(new Option("\tProbability of reinfection of individuals"
                + "\n\tthat have already been infected by covid.", "J", 1, "-J <string>"));
        newVector.addElement(new Option("\tPercentage of super-spreading individuals"
                + "\n\tin the population.", "K", 1, "-K <string>"));
        newVector.addElement(new Option("\tPercentage of death of individuals"
                + "\n\tthat have been infected.", "M", 1, "-M <string>"));
        
        return newVector.elements();
    }

    private String startSetToString() {
        StringBuffer FString = new StringBuffer();
        boolean didPrint;

        if (m_starting == null) {
            return getStartSet();
        }
        
        for (int i = 0; i < m_starting.length; i++) {
            didPrint = false;

            if ((m_hasClass == false) || (m_hasClass == true && i != m_classIndex)) {
                FString.append((m_starting[i] + 1));
                didPrint = true;
            }

            if (i == (m_starting.length - 1)) {
                FString.append("");
            } else {
                if (didPrint) {
                    FString.append(",");
                }
            }
        }

        return FString.toString();
    }
    
    private int[] stringToArrayInt(String s, String atributo){
        if(s.equals("")){
            return null;
        }
        
        String[] stringArray = s.split(",");
        
        if(stringArray.length != numStrains){
            JOptionPane.showMessageDialog(null, "The " + atributo + " size is different from the number of strains");
        }
        
        int[] salida = new int[stringArray.length];

        for(int i = 0; i < stringArray.length; i++){
            salida[i] = Integer.parseInt(stringArray[i].trim());
        }
        
        return salida;
    }
    
    private double[] stringToArrayDouble(String s, String atributo){
        if(s.equals("")){
            return null;
        }
        
        String[] stringArray = s.split(",");
        
        if(stringArray.length != numStrains){
            JOptionPane.showMessageDialog(null, "The " + atributo + " size is different from the number of strains");
        }
        
        double[] salida = new double[stringArray.length];

        for(int i = 0; i < stringArray.length; i++){
            salida[i] = Double.parseDouble(stringArray[i].trim());
        }
        
        return salida;
    }
    
    private int[] defaultValuesInt(int[] array, int valor){
        for(int i = 0; i < numStrains; i++){
            array[i] = valor;
        }
        
        return array;
    }
    
    private double[] defaultValuesDouble(double[] array, double valor){
        for(int i = 0; i < numStrains; i++){
            array[i] = valor;
        }
        
        return array;
    }

    public String startSetTipText() {
        return "Set the start point for the search. This is specified as a comma "
                + "seperated list off attribute indexes starting at 1. It can include "
                + "ranges. Eg. 1,2,5-9,17.";
    }
    
    public String iterationsTipText() {
        return "Maximum number of iterations "
                + "considered for the algorithm to converge.";
    }
    
    public String numStrainsTipText() {
        return "Number of strains "
                + "considered so that there is diversity of solutions.";
    }
    
    public String seedsTipText() {
        return "Initialize the seeds of each strain "
                + "considered in the execution.";
    }
    
    public String minSpreadTipText() {
        return "Minimum number of people infected by an individual "
                + "when propagation occurs without being super-propagating.";
    }
    
    public String maxSpreadTipText() {
        return "Maximum number of people infected by an individual "
                + "when propagation occurs without being super-propagating.";
    }
    
    public String minSuperspreadTipText() {
        return "Minimum number of people infected by an individual "
                + "when propagation occurs being super-propagating.";
    }
    
    public String maxSuperspreadTipText() {
        return "Maximum number of people infected by an individual "
                + "when propagation occurs being super-propagating.";
    }
    
    public String socialDistancingTipText() {
        return "Distance between individuals between whom "
                + "produces propagation.";
    }
    
    public String pIsolationTipText() {
        return "Isolation probability "
                + "between individuals whether they are infected or not.";
    }

    public String pTravelTipText() {
        return "Probability that individuals perform "
                + "trips are infected or not.";
    }
    
    public String pReinfectionTipText() {
        return "Probability of reinfection of individuals "
                + "that have already been infected by covid.";
    }
    
    public String superspreaderPercTipText() {
        return "Percentage of super-spreading individuals "
                + "in the population.";
    }
    
    public String deathPercTipText() {
        return "Percentage of death of individuals "
                + "that have been infected.";
    }
    
    //Getters and setters of the options of CVOA
    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getNumStrains() {
        return numStrains;
    }

    public void setNumStrains(int numStrains) {
        this.numStrains = numStrains;
    }

    public String getSeeds() {
        return seeds;
    }

    public void setSeeds(String seeds) {
        this.seeds = seeds;
    }

    public String getMinSpread() {
        return minSpread;
    }

    public void setMinSpread(String minSpread) {
        this.minSpread = minSpread;
    }

    public String getMaxSpread() {
        return maxSpread;
    }

    public void setMaxSpread(String maxSpread) {
        this.maxSpread = maxSpread;
    }

    public String getMinSuperspread() {
        return minSuperspread;
    }

    public void setMinSuperspread(String minSuperspread) {
        this.minSuperspread = minSuperspread;
    }

    public String getMaxSuperspread() {
        return maxSuperspread;
    }

    public void setMaxSuperspread(String maxSuperspread) {
        this.maxSuperspread = maxSuperspread;
    }

    public String getSocialDistancing() {
        return socialDistancing;
    }

    public void setSocialDistancing(String socialDistancing) {
        this.socialDistancing = socialDistancing;
    }

    public String getpIsolation() {
        return pIsolation;
    }

    public void setpIsolation(String pIsolation) {
        this.pIsolation = pIsolation;
    }

    public String getpTravel() {
        return pTravel;
    }

    public void setpTravel(String pTravel) {
        this.pTravel = pTravel;
    }

    public String getpReinfection() {
        return pReinfection;
    }

    public void setpReinfection(String pReinfection) {
        this.pReinfection = pReinfection;
    }

    public String getSuperspreaderPerc() {
        return superspreaderPerc;
    }

    public void setSuperspreaderPerc(String superspreaderPerc) {
        this.superspreaderPerc = superspreaderPerc;
    }

    public String getDeathPerc() {
        return deathPerc;
    }

    public void setDeathPerc(String deathPerc) {
        this.deathPerc = deathPerc;
    }
}