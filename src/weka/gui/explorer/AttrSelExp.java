/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weka.gui.explorer;

import com.orsonpdf.PDFDocument;
import com.orsonpdf.PDFGraphics2D;
import com.orsonpdf.Page;
import java.awt.BorderLayout;
import java.awt.Component;
import static java.awt.Frame.MAXIMIZED_BOTH;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeEvaluator;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.AttributeTransformer;
import weka.attributeSelection.Ranker;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.core.Attribute;
import weka.core.BatchPredictor;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.converters.AbstractFileSaver;
import weka.core.converters.CSVSaver;
import weka.gui.ConverterFileChooser;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.gui.SysErrLog;
import weka.gui.TaskLogger;
import static weka.gui.explorer.ClassifierPanel.setupEval;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import weka.classifiers.evaluation.ConfusionMatrix;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.DatabaseSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;

/**
 *
 * @author Andrés Manuel
 */
public class AttrSelExp extends javax.swing.JPanel implements Explorer.ExplorerPanel, Explorer.LogHandler {

    /**
    * the parent frame
    */
    protected Explorer m_Explorer = null;

    protected Instances m_Instances;

    /**
    * The main set of instances we're playing with
    */
    protected List<Instances> listInstances;

    /*
    * Where the evaluators, search algorithms and classifier are saved to be loaded
    */
    protected List<Object> listEvaluators;
    protected List<Object> listSearchAlgorithms;
    protected List<Object> listClassifier;
    
    /*
    * Save the classPositive for datasets
    */
    protected List<Integer> listClassPositive;
    
    /**
    * Lets the user configure the attribute evaluator
    */
    protected GenericObjectEditor m_AttributeEvaluatorEditor = new GenericObjectEditor();

    /**
    * The panel showing the current attribute evaluation method
    */
    protected PropertyPanel m_AEEPanel = new PropertyPanel(m_AttributeEvaluatorEditor);

    /**
    * Lets the user configure the search method
    */
    protected GenericObjectEditor m_AttributeSearchEditor = new GenericObjectEditor();

    /**
    * The panel showing the current search method
    */
    protected PropertyPanel m_ASEPanel = new PropertyPanel(m_AttributeSearchEditor);

    /**
    * Lets the user configure the classifier.
    */
    protected GenericObjectEditor m_ClassifierEditor = new GenericObjectEditor();

    /**
    * The panel showing the current classifier selection.
    */
    protected PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor);

    /**
    * Alters the enabled/disabled status of elements associated with each radio
    * button
    */
    ActionListener m_RadioListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateRadioLinks();
        }
    };
    
    /** A thread that attribute selection runs in */
    protected Thread m_RunThread;

    /** The destination for log/status messages */
    protected weka.gui.Logger m_Log = new SysErrLog();
  
    /*
    * To easily add rows and columns to the tables
    */
    protected DefaultTableModel datasetsTableModel;
    protected DefaultTableModel featuresTableModel;
    protected DefaultTableModel classifierTableModel;
    protected DefaultTableModel predictionsTableModel;
    protected DefaultTableModel metricsTableModel;
    protected DefaultTableModel captionTableModel;
    protected DefaultTableModel statisticalTestTableModel;

    /*
    * To easily add rows to the lists
    */
    protected DefaultListModel datasetsListModel = new DefaultListModel();
    protected DefaultListModel evaluatorListModel = new DefaultListModel();
    protected DefaultListModel searchListModel = new DefaultListModel();
    protected DefaultListModel classifierListModel = new DefaultListModel();
    
    /*
    * Attributes required to execute and obtain the results
    */
    protected ExecutorService executor;
    protected List<Future<ResultsAttrSelExp>> resultsAttrSelExp;
    
    //HashMap with the name of the evaluators, searchs and classifiers
    Map<String, String> captionEV = new HashMap<String, String>();
    Map<String, String> captionSE = new HashMap<String, String>();
    Map<String, String> captionCL = new HashMap<String, String>();
    
    /*
    * To facilitate the access of table data
    */
    Instances instPredictions;
    Instances instMetrics;
    
    /*
    * Graph Metrics
    */
    JFreeChart chart;
    
    SwingWorker worker;
    /**
    * Creates new form AttrSelExp
    */
    public AttrSelExp() {
        initComponents();

        //tables of the Jpanel
        datasetsTableModel = (DefaultTableModel) datasetsTable.getModel();
        TableRowSorter<DefaultTableModel> sorterDatasetsTable = new TableRowSorter<>(datasetsTableModel);
        datasetsTable.setRowSorter(sorterDatasetsTable);
        featuresTableModel = (DefaultTableModel) featuresTable.getModel();
        classifierTableModel = (DefaultTableModel) classifierTable.getModel();
        predictionsTableModel = (DefaultTableModel) predictionsTable.getModel();
        metricsTableModel = (DefaultTableModel) metricsTable.getModel();
        captionTableModel = (DefaultTableModel) captionTable.getModel();
        statisticalTestTableModel = (DefaultTableModel) statisticalTestTable.getModel();
        
        //lists of the Jpanel
        datasetsList.setModel(datasetsListModel);
        evaluatorList.setModel(evaluatorListModel);
        searchList.setModel(searchListModel);
        classifierList.setModel(classifierListModel);
        
        //other attributes for save things
        listInstances = new ArrayList<>();
        listEvaluators = new ArrayList<>();
        listSearchAlgorithms = new ArrayList<>();
        listClassifier = new ArrayList<>();
        listClassPositive = new ArrayList<>();
        progressExp.setValue(0);
        numThreadsTextField.setText("4");
        
        //For the percentage to appear in the progressBar
        progressExp.setStringPainted(true);
        
        evaluatorAndSearchPanels();
        classifierPanel();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        validationRadioBtn = new javax.swing.ButtonGroup();
        attrSelExpTabs = new javax.swing.JTabbedPane();
        experiment = new javax.swing.JPanel();
        datasets = new javax.swing.JPanel();
        addFileBtn = new javax.swing.JButton();
        addFolderBtn = new javax.swing.JButton();
        datasetsScrollPane = new javax.swing.JScrollPane();
        datasetsTable = new javax.swing.JTable();
        loadBtnDataset = new javax.swing.JButton();
        removeBtnDataset = new javax.swing.JButton();
        selectionClassCombo = new javax.swing.JComboBox<>();
        selectionPositiveClassCombo = new javax.swing.JComboBox<>();
        classDatasetsLabel = new javax.swing.JLabel();
        positiveClassDatasetsLabel = new javax.swing.JLabel();
        featureSelection = new javax.swing.JPanel();
        evaluatorLabel = new javax.swing.JLabel();
        evaluatorPanel = new javax.swing.JPanel();
        searchLabel = new javax.swing.JLabel();
        searchPanel = new javax.swing.JPanel();
        addFeatureBtn = new javax.swing.JButton();
        featureScrollPane = new javax.swing.JScrollPane();
        featuresTable = new javax.swing.JTable();
        actionsFeatureLabel = new javax.swing.JLabel();
        loadBtnFeature = new javax.swing.JButton();
        removeBtnFeature = new javax.swing.JButton();
        classifier = new javax.swing.JPanel();
        classifierLabel = new javax.swing.JLabel();
        classifierSelectionPanel = new javax.swing.JPanel();
        addClassifierBtn = new javax.swing.JButton();
        classifierScrollPane = new javax.swing.JScrollPane();
        classifierTable = new javax.swing.JTable();
        actionsClassifierLabel = new javax.swing.JLabel();
        loadBtnClassifier = new javax.swing.JButton();
        removeBtnClassifier = new javax.swing.JButton();
        validation = new javax.swing.JPanel();
        holdOutSplitBtn = new javax.swing.JRadioButton();
        holdOutSplitTextField = new javax.swing.JTextField();
        holdOutSplitLabel = new javax.swing.JLabel();
        crossValidationBtn = new javax.swing.JRadioButton();
        crossValidationTextField = new javax.swing.JTextField();
        crossValidationLabel = new javax.swing.JLabel();
        leaveOneOutBtn = new javax.swing.JRadioButton();
        preserveOrderCheckbox = new javax.swing.JCheckBox();
        runPanel = new javax.swing.JPanel();
        progressExp = new javax.swing.JProgressBar();
        runExpBtn = new javax.swing.JButton();
        stopExpBtn = new javax.swing.JButton();
        numThreadsLabel = new javax.swing.JLabel();
        numThreadsTextField = new javax.swing.JTextField();
        experimentPanel = new javax.swing.JPanel();
        nameExpLabel = new javax.swing.JLabel();
        nameExpTextField = new javax.swing.JTextField();
        saveExpBtn = new javax.swing.JButton();
        loadExpBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        results = new javax.swing.JPanel();
        predictions = new javax.swing.JPanel();
        predictionsScrollPane = new javax.swing.JScrollPane();
        predictionsTable = new javax.swing.JTable();
        datasetsListScrollPane = new javax.swing.JScrollPane();
        datasetsList = new javax.swing.JList<>();
        evaluatorListScrollPane = new javax.swing.JScrollPane();
        evaluatorList = new javax.swing.JList<>();
        searchListScrollPane = new javax.swing.JScrollPane();
        searchList = new javax.swing.JList<>();
        classifierListScrollPane = new javax.swing.JScrollPane();
        classifierList = new javax.swing.JList<>();
        attributesTextField = new javax.swing.JTextField();
        saveCSVPredictionsBtn = new javax.swing.JButton();
        saveARFFPredictionsBtn = new javax.swing.JButton();
        loadDatasetPredictionsBtn = new javax.swing.JButton();
        updatePredictionsBtn = new javax.swing.JButton();
        saveBBDDBtn = new javax.swing.JButton();
        loadBBDDBtn = new javax.swing.JButton();
        attributesLabel = new javax.swing.JLabel();
        datasetsLabel = new javax.swing.JLabel();
        evaluatorsLabel = new javax.swing.JLabel();
        searchsLabel = new javax.swing.JLabel();
        classifiersLabel = new javax.swing.JLabel();
        nomenclaturePanel = new javax.swing.JPanel();
        captionScrollPane = new javax.swing.JScrollPane();
        captionTable = new javax.swing.JTable();
        metrics = new javax.swing.JPanel();
        metricsScrollPane = new javax.swing.JScrollPane();
        metricsTable = new javax.swing.JTable();
        saveCSVMetricsBtn = new javax.swing.JButton();
        saveARFFMetricsBtn = new javax.swing.JButton();
        loadDatasetMetricsBtn = new javax.swing.JButton();
        classificationMetricsLabel = new javax.swing.JLabel();
        regressionMetricsLabel = new javax.swing.JLabel();
        accuracyMetricsCheckBox = new javax.swing.JCheckBox();
        precisionMetricsCheckBox = new javax.swing.JCheckBox();
        recallMetricsCheckBox = new javax.swing.JCheckBox();
        fMeasureMetricsCheckBox = new javax.swing.JCheckBox();
        kappaMetricsCheckBox = new javax.swing.JCheckBox();
        mccMetricsCheckBox = new javax.swing.JCheckBox();
        aucMetricsCheckBox = new javax.swing.JCheckBox();
        maeMetricsCheckBox = new javax.swing.JCheckBox();
        mseMetricsCheckBox = new javax.swing.JCheckBox();
        rmseMetricsCheckBox = new javax.swing.JCheckBox();
        mapeMetricsCheckBox = new javax.swing.JCheckBox();
        r2MetricsCheckBox = new javax.swing.JCheckBox();
        saveLatexMetricsBtn = new javax.swing.JButton();
        graph = new javax.swing.JPanel();
        barChartPanel = new javax.swing.JPanel();
        metricGraphLabel = new javax.swing.JLabel();
        metricGraphComboBox = new javax.swing.JComboBox<>();
        xAxis1Label = new javax.swing.JLabel();
        xAxis1ComboBox = new javax.swing.JComboBox<>();
        xAxis2Label = new javax.swing.JLabel();
        xAxis2ComboBox = new javax.swing.JComboBox<>();
        savePDFGraphBtn = new javax.swing.JButton();
        savePNGGraphBtn = new javax.swing.JButton();
        updateGraphBtn = new javax.swing.JButton();
        labelFilter1 = new javax.swing.JLabel();
        labelFilter2 = new javax.swing.JLabel();
        filterGraph1ComboBox = new javax.swing.JComboBox<>();
        filterGraph2ComboBox = new javax.swing.JComboBox<>();
        statisticalTestsPanel = new javax.swing.JPanel();
        metricStatisLabel = new javax.swing.JLabel();
        numDatasetsStatisLabel = new javax.swing.JLabel();
        numSamStatisLabel = new javax.swing.JLabel();
        groupsLOOLabel = new javax.swing.JLabel();
        metricStatisComboBox = new javax.swing.JComboBox<>();
        numDatasetsStatisValue = new javax.swing.JLabel();
        numSamStatisValue = new javax.swing.JLabel();
        filterStatisLabel = new javax.swing.JLabel();
        filterStatisComboBox = new javax.swing.JComboBox<>();
        saveCSVStatisBtn = new javax.swing.JButton();
        putPreprocessStatisBtn = new javax.swing.JButton();
        statisticalTestScrollPane = new javax.swing.JScrollPane();
        statisticalTestTable = new javax.swing.JTable();
        messageNormTestLabel = new javax.swing.JLabel();
        valueGroupsLOOTextField = new javax.swing.JTextField();
        ropeLabel = new javax.swing.JLabel();
        ropeTextField = new javax.swing.JTextField();
        messagesStatis = new javax.swing.JLabel();

        validationRadioBtn.add(holdOutSplitBtn);
        validationRadioBtn.add(crossValidationBtn);
        validationRadioBtn.add(leaveOneOutBtn);

        setLayout(new java.awt.BorderLayout());

        experiment.setLayout(new java.awt.GridBagLayout());

        datasets.setLayout(new java.awt.GridBagLayout());

        addFileBtn.setText("Add");
        addFileBtn.setToolTipText("Add the file in the tab Preprocess to table");
        addFileBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFileBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        datasets.add(addFileBtn, gridBagConstraints);

        addFolderBtn.setText("Add folder...");
        addFolderBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFolderBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        datasets.add(addFolderBtn, gridBagConstraints);

        datasetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Relation", "Instances", "Attributes", "Number classes", "Class", "Positive class", "Index"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        datasetsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                datasetsTableMouseClicked(evt);
            }
        });
        datasetsScrollPane.setViewportView(datasetsTable);
        if (datasetsTable.getColumnModel().getColumnCount() > 0) {
            datasetsTable.getColumnModel().getColumn(6).setMinWidth(0);
            datasetsTable.getColumnModel().getColumn(6).setPreferredWidth(0);
            datasetsTable.getColumnModel().getColumn(6).setMaxWidth(0);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        datasets.add(datasetsScrollPane, gridBagConstraints);

        loadBtnDataset.setText("Put to Preprocess");
        loadBtnDataset.setToolTipText("Load the selected row of the table in the tab Preprocess");
        loadBtnDataset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadBtnDatasetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        datasets.add(loadBtnDataset, gridBagConstraints);

        removeBtnDataset.setText("Remove");
        removeBtnDataset.setToolTipText("Remove the selected row of the table");
        removeBtnDataset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeBtnDatasetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        datasets.add(removeBtnDataset, gridBagConstraints);

        selectionClassCombo.setToolTipText("Select the attribute to use as the class");
        selectionClassCombo.setMinimumSize(new java.awt.Dimension(52, 20));
        selectionClassCombo.setPreferredSize(new java.awt.Dimension(52, 20));
        selectionClassCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                selectionClassComboItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        datasets.add(selectionClassCombo, gridBagConstraints);

        selectionPositiveClassCombo.setToolTipText("Select the index to use as the positive class");
        selectionPositiveClassCombo.setMinimumSize(new java.awt.Dimension(48, 20));
        selectionPositiveClassCombo.setPreferredSize(new java.awt.Dimension(48, 20));
        selectionPositiveClassCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                selectionPositiveClassComboItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        datasets.add(selectionPositiveClassCombo, gridBagConstraints);

        classDatasetsLabel.setText("Class: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        datasets.add(classDatasetsLabel, gridBagConstraints);

        positiveClassDatasetsLabel.setText("Positive class: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        datasets.add(positiveClassDatasetsLabel, gridBagConstraints);

        datasets.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Datasets"),
            BorderFactory.createEmptyBorder(0, 5, 5, 5)));

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    experiment.add(datasets, gridBagConstraints);

    featureSelection.setLayout(new java.awt.GridBagLayout());

    evaluatorLabel.setText("Evaluator:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    featureSelection.add(evaluatorLabel, gridBagConstraints);

    evaluatorPanel.setLayout(new java.awt.BorderLayout());
    evaluatorPanel.add(m_AEEPanel, BorderLayout.CENTER);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    featureSelection.add(evaluatorPanel, gridBagConstraints);

    searchLabel.setText("Search:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    featureSelection.add(searchLabel, gridBagConstraints);

    searchPanel.setLayout(new java.awt.BorderLayout());
    searchPanel.add(m_ASEPanel, BorderLayout.CENTER);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    featureSelection.add(searchPanel, gridBagConstraints);

    addFeatureBtn.setText("Add");
    addFeatureBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            addFeatureBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    featureSelection.add(addFeatureBtn, gridBagConstraints);

    featuresTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Evaluator", "Search"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    featureScrollPane.setViewportView(featuresTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weighty = 1.0;
    featureSelection.add(featureScrollPane, gridBagConstraints);

    actionsFeatureLabel.setText("Actions: ");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    featureSelection.add(actionsFeatureLabel, gridBagConstraints);

    loadBtnFeature.setText("Load");
    loadBtnFeature.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadBtnFeatureActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    featureSelection.add(loadBtnFeature, gridBagConstraints);

    removeBtnFeature.setText("Remove");
    removeBtnFeature.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            removeBtnFeatureActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    featureSelection.add(removeBtnFeature, gridBagConstraints);

    featureSelection.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Feature Selection"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 1;
gridBagConstraints.gridy = 0;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
experiment.add(featureSelection, gridBagConstraints);

classifier.setLayout(new java.awt.GridBagLayout());

classifierLabel.setText("Classifiers:");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 0;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
classifier.add(classifierLabel, gridBagConstraints);

classifierSelectionPanel.setLayout(new java.awt.BorderLayout());
classifierSelectionPanel.add(m_CEPanel, BorderLayout.CENTER);
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 1;
gridBagConstraints.gridy = 0;
gridBagConstraints.gridwidth = 3;
gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
gridBagConstraints.weightx = 1.0;
classifier.add(classifierSelectionPanel, gridBagConstraints);

addClassifierBtn.setText("Add");
addClassifierBtn.addActionListener(new java.awt.event.ActionListener() {
    public void actionPerformed(java.awt.event.ActionEvent evt) {
        addClassifierBtnActionPerformed(evt);
    }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    classifier.add(addClassifierBtn, gridBagConstraints);

    classifierTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Classifier"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    classifierScrollPane.setViewportView(classifierTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weighty = 1.0;
    classifier.add(classifierScrollPane, gridBagConstraints);

    actionsClassifierLabel.setText("Actions: ");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    classifier.add(actionsClassifierLabel, gridBagConstraints);

    loadBtnClassifier.setText("Load");
    loadBtnClassifier.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadBtnClassifierActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    classifier.add(loadBtnClassifier, gridBagConstraints);

    removeBtnClassifier.setText("Remove");
    removeBtnClassifier.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            removeBtnClassifierActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    classifier.add(removeBtnClassifier, gridBagConstraints);

    classifier.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Classifier"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 1;
gridBagConstraints.gridheight = 2;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
experiment.add(classifier, gridBagConstraints);

validation.setLayout(new java.awt.GridBagLayout());

validationRadioBtn.add(holdOutSplitBtn);
holdOutSplitBtn.setSelected(ExplorerDefaults.getASTestMode() == 0);
holdOutSplitBtn.setText("Hold-out split");
holdOutSplitBtn.setToolTipText("Perform % for training and the rest for testing");
holdOutSplitBtn.addActionListener(m_RadioListener);
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 0;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
validation.add(holdOutSplitBtn, gridBagConstraints);

holdOutSplitTextField.setText("70");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 1;
gridBagConstraints.gridy = 0;
validation.add(holdOutSplitTextField, gridBagConstraints);

holdOutSplitLabel.setText("%");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 2;
gridBagConstraints.gridy = 0;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
gridBagConstraints.weightx = 1.0;
validation.add(holdOutSplitLabel, gridBagConstraints);

validationRadioBtn.add(crossValidationBtn);
crossValidationBtn.setSelected(ExplorerDefaults.getASTestMode() == 1);
crossValidationBtn.setText("Cross validation");
crossValidationBtn.setToolTipText("Perform a n-fold cross-validation");
crossValidationBtn.addActionListener(m_RadioListener);
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 1;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
validation.add(crossValidationBtn, gridBagConstraints);

crossValidationTextField.setText("10");
crossValidationTextField.setEnabled(false);
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 1;
gridBagConstraints.gridy = 1;
validation.add(crossValidationTextField, gridBagConstraints);

crossValidationLabel.setText("folds");
crossValidationLabel.setEnabled(false);
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 2;
gridBagConstraints.gridy = 1;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
gridBagConstraints.weightx = 1.0;
validation.add(crossValidationLabel, gridBagConstraints);

validationRadioBtn.add(leaveOneOutBtn);
leaveOneOutBtn.setSelected(ExplorerDefaults.getASTestMode() == 1);
leaveOneOutBtn.setText("Leave One Out");
leaveOneOutBtn.setToolTipText("Perform training with all instances except one, with which the test will be executed");
leaveOneOutBtn.addActionListener(m_RadioListener);
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 2;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
validation.add(leaveOneOutBtn, gridBagConstraints);

preserveOrderCheckbox.setText("Preserve order for % split");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 3;
gridBagConstraints.gridy = 0;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
gridBagConstraints.weightx = 1.0;
validation.add(preserveOrderCheckbox, gridBagConstraints);

validation.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createTitledBorder("Validation"),
    BorderFactory.createEmptyBorder(0, 5, 5, 5)));

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    experiment.add(validation, gridBagConstraints);

    runPanel.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(progressExp, gridBagConstraints);

    runExpBtn.setText("Run");
    runExpBtn.setToolTipText("Starts attribute selection experimenter");
    runExpBtn.setEnabled(false);
    runExpBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            runExpBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(runExpBtn, gridBagConstraints);

    stopExpBtn.setText("Stop");
    stopExpBtn.setToolTipText("Stop attribute selection experimenter");
    stopExpBtn.setEnabled(false);
    stopExpBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            stopExpBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(stopExpBtn, gridBagConstraints);

    numThreadsLabel.setText("Threads:");
    numThreadsLabel.setToolTipText("Number of threads with which it will be executed");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(numThreadsLabel, gridBagConstraints);

    numThreadsTextField.setToolTipText("Number of threads with which it will be executed");
    numThreadsTextField.setMinimumSize(new java.awt.Dimension(20, 22));
    numThreadsTextField.setPreferredSize(new java.awt.Dimension(50, 22));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(numThreadsTextField, gridBagConstraints);

    experimentPanel.setPreferredSize(new java.awt.Dimension(120, 25));
    experimentPanel.setLayout(new java.awt.GridBagLayout());

    nameExpLabel.setText("Name:   ");
    nameExpLabel.setMaximumSize(new java.awt.Dimension(50, 14));
    nameExpLabel.setMinimumSize(new java.awt.Dimension(50, 14));
    nameExpLabel.setPreferredSize(new java.awt.Dimension(50, 14));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    experimentPanel.add(nameExpLabel, gridBagConstraints);

    nameExpTextField.setMinimumSize(new java.awt.Dimension(100, 30));
    nameExpTextField.setPreferredSize(new java.awt.Dimension(140, 30));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    experimentPanel.add(nameExpTextField, gridBagConstraints);

    saveExpBtn.setText("Save Experiment");
    saveExpBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveExpBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    experimentPanel.add(saveExpBtn, gridBagConstraints);

    loadExpBtn.setText("Load Experiment");
    loadExpBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadExpBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    experimentPanel.add(loadExpBtn, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    runPanel.add(experimentPanel, gridBagConstraints);

    javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
    jPanel2.setLayout(jPanel2Layout);
    jPanel2Layout.setHorizontalGroup(
        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 0, Short.MAX_VALUE)
    );
    jPanel2Layout.setVerticalGroup(
        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 0, Short.MAX_VALUE)
    );

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    runPanel.add(jPanel2, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    experiment.add(runPanel, gridBagConstraints);

    attrSelExpTabs.addTab("tab1", experiment);

    results.setLayout(new java.awt.GridBagLayout());

    predictions.setMinimumSize(new java.awt.Dimension(505, 209));
    predictions.setPreferredSize(new java.awt.Dimension(1336, 509));
    predictions.setLayout(new java.awt.GridBagLayout());

    predictionsScrollPane.setMinimumSize(new java.awt.Dimension(600, 23));
    predictionsScrollPane.setPreferredSize(new java.awt.Dimension(452, 402));

    predictionsTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Actual", "Predicted", "Dataset", "Evaluator", "Search", "Classifier"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.Double.class, java.lang.Double.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    predictionsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    predictionsScrollPane.setViewportView(predictionsTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.gridheight = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    predictions.add(predictionsScrollPane, gridBagConstraints);

    datasetsListScrollPane.setMaximumSize(new java.awt.Dimension(300, 32767));
    datasetsListScrollPane.setMinimumSize(new java.awt.Dimension(100, 23));
    datasetsListScrollPane.setName(""); // NOI18N
    datasetsListScrollPane.setPreferredSize(new java.awt.Dimension(158, 130));

    datasetsListScrollPane.setViewportView(datasetsList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(datasetsListScrollPane, gridBagConstraints);

    evaluatorListScrollPane.setMaximumSize(new java.awt.Dimension(300, 32767));
    evaluatorListScrollPane.setMinimumSize(new java.awt.Dimension(100, 23));
    evaluatorListScrollPane.setPreferredSize(new java.awt.Dimension(158, 130));
    evaluatorListScrollPane.setViewportView(evaluatorList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(evaluatorListScrollPane, gridBagConstraints);

    searchListScrollPane.setMaximumSize(new java.awt.Dimension(300, 32767));
    searchListScrollPane.setMinimumSize(new java.awt.Dimension(100, 23));
    searchListScrollPane.setPreferredSize(new java.awt.Dimension(158, 130));

    searchListScrollPane.setViewportView(searchList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(searchListScrollPane, gridBagConstraints);

    classifierListScrollPane.setMaximumSize(new java.awt.Dimension(300, 32767));
    classifierListScrollPane.setMinimumSize(new java.awt.Dimension(100, 23));
    classifierListScrollPane.setPreferredSize(new java.awt.Dimension(158, 130));

    classifierListScrollPane.setViewportView(classifierList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(classifierListScrollPane, gridBagConstraints);

    attributesTextField.setToolTipText("The input format must be number followed by like, eg: 1, 2");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(attributesTextField, gridBagConstraints);

    saveCSVPredictionsBtn.setText("Save to CSV");
    saveCSVPredictionsBtn.setToolTipText("Save to CSV");
    saveCSVPredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveCSVPredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    predictions.add(saveCSVPredictionsBtn, gridBagConstraints);

    saveARFFPredictionsBtn.setText("Save to ARFF");
    saveARFFPredictionsBtn.setToolTipText("Save to ARFF");
    saveARFFPredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveARFFPredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    predictions.add(saveARFFPredictionsBtn, gridBagConstraints);

    loadDatasetPredictionsBtn.setText("Put to Preprocess");
    loadDatasetPredictionsBtn.setToolTipText("Put to Preprocess");
    loadDatasetPredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadDatasetPredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(loadDatasetPredictionsBtn, gridBagConstraints);

    updatePredictionsBtn.setText("Update");
    updatePredictionsBtn.setToolTipText("Update");
    updatePredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            updatePredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 3;
    predictions.add(updatePredictionsBtn, gridBagConstraints);

    saveBBDDBtn.setText("Save to DB");
    saveBBDDBtn.setToolTipText("Save predictions and metrics to the database");
    saveBBDDBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveBBDDBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(saveBBDDBtn, gridBagConstraints);

    loadBBDDBtn.setText("Load DB");
    loadBBDDBtn.setToolTipText("Load data from a predictions and metrics database");
    loadBBDDBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadBBDDBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(loadBBDDBtn, gridBagConstraints);

    attributesLabel.setText("Attributes:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 3;
    predictions.add(attributesLabel, gridBagConstraints);

    datasetsLabel.setText("Datasets");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 1;
    predictions.add(datasetsLabel, gridBagConstraints);

    evaluatorsLabel.setText("Evaluators");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 1;
    predictions.add(evaluatorsLabel, gridBagConstraints);

    searchsLabel.setText("Searchs");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 1;
    predictions.add(searchsLabel, gridBagConstraints);

    classifiersLabel.setText("Classifiers");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 1;
    predictions.add(classifiersLabel, gridBagConstraints);

    nomenclaturePanel.setLayout(new java.awt.GridBagLayout());

    captionTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Name", "Definition"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    captionScrollPane.setViewportView(captionTable);
    if (captionTable.getColumnModel().getColumnCount() > 0) {
        captionTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        captionTable.getColumnModel().getColumn(0).setMaxWidth(70);
    }

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    nomenclaturePanel.add(captionScrollPane, gridBagConstraints);

    nomenclaturePanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Nomenclature"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 5;
gridBagConstraints.gridy = 0;
gridBagConstraints.gridwidth = 4;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
predictions.add(nomenclaturePanel, gridBagConstraints);

predictions.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createTitledBorder("Predictions"),
    BorderFactory.createEmptyBorder(0, 5, 5, 5)));

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    results.add(predictions, gridBagConstraints);

    metrics.setMinimumSize(new java.awt.Dimension(505, 180));
    metrics.setName(""); // NOI18N
    metrics.setPreferredSize(new java.awt.Dimension(705, 508));
    metrics.setLayout(new java.awt.GridBagLayout());

    metricsTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Dataset", "Evaluator", "Search", "Classifier", "NumAttributes"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    metricsTable.setToolTipText("The data in the numAttributes column of the metrics table is obtained by filtering the entire dataset if the CV or LVO validation method has been chosen.");
    metricsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    metricsScrollPane.setViewportView(metricsTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.gridheight = 9;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    metrics.add(metricsScrollPane, gridBagConstraints);

    saveCSVMetricsBtn.setText("Save to CSV");
    saveCSVMetricsBtn.setToolTipText("Save to CSV");
    saveCSVMetricsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveCSVMetricsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    metrics.add(saveCSVMetricsBtn, gridBagConstraints);

    saveARFFMetricsBtn.setText("Save to ARFF");
    saveARFFMetricsBtn.setToolTipText("Save to ARFF");
    saveARFFMetricsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveARFFMetricsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    metrics.add(saveARFFMetricsBtn, gridBagConstraints);

    loadDatasetMetricsBtn.setText("Put to Preprocess");
    loadDatasetMetricsBtn.setToolTipText("Put to Preprocess");
    loadDatasetMetricsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadDatasetMetricsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(loadDatasetMetricsBtn, gridBagConstraints);

    classificationMetricsLabel.setText("Classification");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    metrics.add(classificationMetricsLabel, gridBagConstraints);

    regressionMetricsLabel.setText("Regression");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    metrics.add(regressionMetricsLabel, gridBagConstraints);

    accuracyMetricsCheckBox.setText("Accuracy");
    accuracyMetricsCheckBox.setEnabled(false);
    accuracyMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            accuracyMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(accuracyMetricsCheckBox, gridBagConstraints);

    precisionMetricsCheckBox.setText("Precision");
    precisionMetricsCheckBox.setEnabled(false);
    precisionMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            precisionMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(precisionMetricsCheckBox, gridBagConstraints);

    recallMetricsCheckBox.setText("Recall");
    recallMetricsCheckBox.setEnabled(false);
    recallMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            recallMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(recallMetricsCheckBox, gridBagConstraints);

    fMeasureMetricsCheckBox.setText("F-measure");
    fMeasureMetricsCheckBox.setEnabled(false);
    fMeasureMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            fMeasureMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(fMeasureMetricsCheckBox, gridBagConstraints);

    kappaMetricsCheckBox.setText("Kappa");
    kappaMetricsCheckBox.setEnabled(false);
    kappaMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            kappaMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(kappaMetricsCheckBox, gridBagConstraints);

    mccMetricsCheckBox.setText("MCC");
    mccMetricsCheckBox.setEnabled(false);
    mccMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            mccMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(mccMetricsCheckBox, gridBagConstraints);

    aucMetricsCheckBox.setText("AUC");
    aucMetricsCheckBox.setEnabled(false);
    aucMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            aucMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    metrics.add(aucMetricsCheckBox, gridBagConstraints);

    maeMetricsCheckBox.setText("MAE");
    maeMetricsCheckBox.setEnabled(false);
    maeMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            maeMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(maeMetricsCheckBox, gridBagConstraints);

    mseMetricsCheckBox.setText("MSE");
    mseMetricsCheckBox.setEnabled(false);
    mseMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            mseMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(mseMetricsCheckBox, gridBagConstraints);

    rmseMetricsCheckBox.setText("RMSE");
    rmseMetricsCheckBox.setEnabled(false);
    rmseMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            rmseMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(rmseMetricsCheckBox, gridBagConstraints);

    mapeMetricsCheckBox.setText("MAPE");
    mapeMetricsCheckBox.setEnabled(false);
    mapeMetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            mapeMetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(mapeMetricsCheckBox, gridBagConstraints);

    r2MetricsCheckBox.setText("R2");
    r2MetricsCheckBox.setEnabled(false);
    r2MetricsCheckBox.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            r2MetricsCheckBoxMouseClicked(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(r2MetricsCheckBox, gridBagConstraints);

    saveLatexMetricsBtn.setText("Save to Latex");
    saveLatexMetricsBtn.setToolTipText("Save to Latex");
    saveLatexMetricsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveLatexMetricsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 9;
    metrics.add(saveLatexMetricsBtn, gridBagConstraints);

    metrics.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Metrics"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 1;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
results.add(metrics, gridBagConstraints);

graph.setMinimumSize(new java.awt.Dimension(500, 100));
graph.setPreferredSize(new java.awt.Dimension(1200, 300));
graph.setLayout(new java.awt.GridBagLayout());

barChartPanel.setMinimumSize(new java.awt.Dimension(200, 0));
barChartPanel.setPreferredSize(new java.awt.Dimension(1000, 0));

javax.swing.GroupLayout barChartPanelLayout = new javax.swing.GroupLayout(barChartPanel);
barChartPanel.setLayout(barChartPanelLayout);
barChartPanelLayout.setHorizontalGroup(
    barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
    .addGap(0, 352, Short.MAX_VALUE)
    );
    barChartPanelLayout.setVerticalGroup(
        barChartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 0, Short.MAX_VALUE)
    );

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = 8;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    graph.add(barChartPanel, gridBagConstraints);

    metricGraphLabel.setText("Y-axis:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    graph.add(metricGraphLabel, gridBagConstraints);

    metricGraphComboBox.setMaximumSize(new java.awt.Dimension(98, 20));
    metricGraphComboBox.setMinimumSize(new java.awt.Dimension(98, 19));
    metricGraphComboBox.setPreferredSize(new java.awt.Dimension(98, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    graph.add(metricGraphComboBox, gridBagConstraints);

    xAxis1Label.setText("X-axis (1):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    graph.add(xAxis1Label, gridBagConstraints);

    xAxis1ComboBox.setMaximumSize(new java.awt.Dimension(98, 20));
    xAxis1ComboBox.setMinimumSize(new java.awt.Dimension(98, 19));
    xAxis1ComboBox.setPreferredSize(new java.awt.Dimension(98, 20));
    xAxis1ComboBox.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent evt) {
            xAxis1ComboBoxItemStateChanged(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    graph.add(xAxis1ComboBox, gridBagConstraints);

    xAxis2Label.setText("X-axis (2):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    graph.add(xAxis2Label, gridBagConstraints);

    xAxis2ComboBox.setMaximumSize(new java.awt.Dimension(98, 20));
    xAxis2ComboBox.setMinimumSize(new java.awt.Dimension(98, 19));
    xAxis2ComboBox.setPreferredSize(new java.awt.Dimension(98, 20));
    xAxis2ComboBox.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent evt) {
            xAxis2ComboBoxItemStateChanged(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    graph.add(xAxis2ComboBox, gridBagConstraints);

    savePDFGraphBtn.setText("Save to PDF");
    savePDFGraphBtn.setToolTipText("Save to PDF");
    savePDFGraphBtn.setMaximumSize(new java.awt.Dimension(98, 23));
    savePDFGraphBtn.setMinimumSize(new java.awt.Dimension(98, 21));
    savePDFGraphBtn.setPreferredSize(new java.awt.Dimension(98, 23));
    savePDFGraphBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            savePDFGraphBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    graph.add(savePDFGraphBtn, gridBagConstraints);

    savePNGGraphBtn.setText("Save to PNG");
    savePNGGraphBtn.setToolTipText("Save to PNG");
    savePNGGraphBtn.setMaximumSize(new java.awt.Dimension(98, 23));
    savePNGGraphBtn.setMinimumSize(new java.awt.Dimension(98, 21));
    savePNGGraphBtn.setPreferredSize(new java.awt.Dimension(98, 23));
    savePNGGraphBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            savePNGGraphBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    graph.add(savePNGGraphBtn, gridBagConstraints);

    updateGraphBtn.setText("Update");
    updateGraphBtn.setToolTipText("Update");
    updateGraphBtn.setMaximumSize(new java.awt.Dimension(85, 23));
    updateGraphBtn.setMinimumSize(new java.awt.Dimension(85, 21));
    updateGraphBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            updateGraphBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    graph.add(updateGraphBtn, gridBagConstraints);

    labelFilter1.setText("Filter (1):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    graph.add(labelFilter1, gridBagConstraints);

    labelFilter2.setText("Filter (2):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    graph.add(labelFilter2, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    graph.add(filterGraph1ComboBox, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    graph.add(filterGraph2ComboBox, gridBagConstraints);

    graph.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Graph"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 1;
gridBagConstraints.gridy = 1;
gridBagConstraints.gridwidth = 2;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
results.add(graph, gridBagConstraints);

statisticalTestsPanel.setLayout(new java.awt.GridBagLayout());

metricStatisLabel.setText("Metric:");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 0;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
statisticalTestsPanel.add(metricStatisLabel, gridBagConstraints);

numDatasetsStatisLabel.setText("Number of datasets:");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 1;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
statisticalTestsPanel.add(numDatasetsStatisLabel, gridBagConstraints);

numSamStatisLabel.setText("Number of samples:");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 2;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
statisticalTestsPanel.add(numSamStatisLabel, gridBagConstraints);

groupsLOOLabel.setText("Groups for leave-one-out: ");
gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 0;
gridBagConstraints.gridy = 3;
gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
statisticalTestsPanel.add(groupsLOOLabel, gridBagConstraints);

metricStatisComboBox.setMinimumSize(new java.awt.Dimension(98, 20));
metricStatisComboBox.setPreferredSize(new java.awt.Dimension(98, 20));
metricStatisComboBox.addItemListener(new java.awt.event.ItemListener() {
    public void itemStateChanged(java.awt.event.ItemEvent evt) {
        metricStatisComboBoxItemStateChanged(evt);
    }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    statisticalTestsPanel.add(metricStatisComboBox, gridBagConstraints);

    numDatasetsStatisValue.setText("0");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    statisticalTestsPanel.add(numDatasetsStatisValue, gridBagConstraints);

    numSamStatisValue.setText("0");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    statisticalTestsPanel.add(numSamStatisValue, gridBagConstraints);

    filterStatisLabel.setText("Filter:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    statisticalTestsPanel.add(filterStatisLabel, gridBagConstraints);

    filterStatisComboBox.setToolTipText("Filter test results can include All (all results), just the Positives (1 or 2) or the Negatives (0).");
    filterStatisComboBox.setMinimumSize(new java.awt.Dimension(98, 20));
    filterStatisComboBox.setPreferredSize(new java.awt.Dimension(98, 20));
    filterStatisComboBox.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent evt) {
            filterStatisComboBoxItemStateChanged(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    statisticalTestsPanel.add(filterStatisComboBox, gridBagConstraints);

    saveCSVStatisBtn.setText("Save to CSV");
    saveCSVStatisBtn.setToolTipText("Save to CSV");
    saveCSVStatisBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveCSVStatisBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 7;
    statisticalTestsPanel.add(saveCSVStatisBtn, gridBagConstraints);

    putPreprocessStatisBtn.setText("Put to Preprocess");
    putPreprocessStatisBtn.setToolTipText("Put to Preprocess");
    putPreprocessStatisBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            putPreprocessStatisBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    statisticalTestsPanel.add(putPreprocessStatisBtn, gridBagConstraints);

    statisticalTestTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Evaluator #1", "Search #1", "Algorithm #1", "Evaluator #2", "Search #2", "Algorithm #2", "p(left)", "p(no)", "p(right)", "Test"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false, false, false, false, false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    statisticalTestTable.setToolTipText("Test result can be 0 (no difference between methods), 1 (the method #1 is better) or 2 (the method #2 is better). ");
    statisticalTestScrollPane.setViewportView(statisticalTestTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    statisticalTestsPanel.add(statisticalTestScrollPane, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    statisticalTestsPanel.add(messageNormTestLabel, gridBagConstraints);

    valueGroupsLOOTextField.setText("30");
    valueGroupsLOOTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            valueGroupsLOOTextFieldActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    statisticalTestsPanel.add(valueGroupsLOOTextField, gridBagConstraints);

    ropeLabel.setText("Rope:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    statisticalTestsPanel.add(ropeLabel, gridBagConstraints);

    ropeTextField.setText("1");
    ropeTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            ropeTextFieldActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    statisticalTestsPanel.add(ropeTextField, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    statisticalTestsPanel.add(messagesStatis, gridBagConstraints);

    statisticalTestsPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Statistical tests"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 2;
gridBagConstraints.gridy = 0;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
results.add(statisticalTestsPanel, gridBagConstraints);

attrSelExpTabs.addTab("tab2", results);

add(attrSelExpTabs, java.awt.BorderLayout.CENTER);
attrSelExpTabs.addTab("Experiment", experiment);
attrSelExpTabs.addTab("Results", results);
}// </editor-fold>//GEN-END:initComponents

    private void addFileBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFileBtnActionPerformed
        m_Log.statusMessage("OK");
        
        if(m_Instances != null){
            m_Instances.setClassIndex(m_Instances.numAttributes() - 1);

            datasetsTableModel.addRow(new Object[]{m_Instances.relationName(), m_Instances.numInstances(), 
                m_Instances.numAttributes() - 1, m_Instances.numClasses(), m_Instances.attribute(m_Instances.numAttributes() - 1).name(), 
                m_Instances.attribute(m_Instances.classIndex()).value(0), listInstances.size()});

            listInstances.add(m_Instances);
            listClassPositive.add(0);
                        
            checkRun();
        }else{
            m_Log.logMessage("There is no dataset previously loaded");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_addFileBtnActionPerformed

    private void removeBtnDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBtnDatasetActionPerformed
        m_Log.statusMessage("OK");
        
        if(datasetsTable.getSelectedRows().length != 0){
            List<Integer> indexRows = new ArrayList<>();
            
            for(int i = datasetsTable.getSelectedRows().length-1; i >= 0; i--){
                indexRows.add((int)datasetsTable.getValueAt(datasetsTable.getSelectedRows()[i], 6));
                datasetsTableModel.removeRow(datasetsTable.convertRowIndexToModel(datasetsTable.getSelectedRows()[i]));
            }
            
            Collections.sort(indexRows);  
            
            for(int i = indexRows.size()-1; i >= 0; i--){
                listInstances.remove((int)indexRows.get(i));
                listClassPositive.remove((int)indexRows.get(i));
            }
            
            for(int j = 0; j < listInstances.size(); j++){
                for(int z = 0; z < datasetsTable.getRowCount(); z++){
                    if(datasetsTable.getValueAt(z, 0).equals(listInstances.get(j).relationName())){
                        datasetsTable.setValueAt(j, z, 6);
                    }
                }
            }
        } else {
            m_Log.logMessage("No row is selected");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_removeBtnDatasetActionPerformed

    private void loadBtnDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBtnDatasetActionPerformed
        m_Log.statusMessage("OK");
        
        if(datasetsTable.getSelectedRow() != -1){
            getExplorer().getPreprocessPanel().setInstances(listInstances.get(datasetsTable.getSelectedRow()));
        }else{
            m_Log.logMessage("No row is selected");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_loadBtnDatasetActionPerformed

    private void addFeatureBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFeatureBtnActionPerformed
        featuresTableModel.addRow(new Object[]{getSpec(m_AttributeEvaluatorEditor.getValue()), getSpec(m_AttributeSearchEditor.getValue())});
        listEvaluators.add(m_AttributeEvaluatorEditor.getValue());
        listSearchAlgorithms.add(m_AttributeSearchEditor.getValue());
        checkRun();
    }//GEN-LAST:event_addFeatureBtnActionPerformed

    private void addClassifierBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addClassifierBtnActionPerformed
        classifierTableModel.addRow(new Object[]{getSpec(m_ClassifierEditor.getValue())});
        listClassifier.add(m_ClassifierEditor.getValue());
        checkRun();
    }//GEN-LAST:event_addClassifierBtnActionPerformed

    private void removeBtnFeatureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBtnFeatureActionPerformed
        m_Log.statusMessage("OK");
        
        if(featuresTable.getSelectedRow() != -1){           
            listEvaluators.remove(featuresTable.getSelectedRow());
            listSearchAlgorithms.remove(featuresTable.getSelectedRow());
            featuresTableModel.removeRow(featuresTable.getSelectedRow());
              
            checkRun();
        }else{
            m_Log.logMessage("No row is selected");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_removeBtnFeatureActionPerformed

    private void loadBtnFeatureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBtnFeatureActionPerformed
        m_Log.statusMessage("OK");
        
        if(featuresTable.getSelectedRow() != -1){
            m_AttributeSearchEditor.setValue(listSearchAlgorithms.get(featuresTable.getSelectedRow()));
            m_AttributeEvaluatorEditor.setValue(listEvaluators.get(featuresTable.getSelectedRow()));
        }else{
            m_Log.logMessage("No row is selected");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_loadBtnFeatureActionPerformed

    private void loadBtnClassifierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBtnClassifierActionPerformed
        m_Log.statusMessage("OK");
        
        if(classifierTable.getSelectedRow() != -1){
            m_ClassifierEditor.setValue(listClassifier.get(classifierTable.getSelectedRow()));
        }else{
            m_Log.logMessage("No row is selected");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_loadBtnClassifierActionPerformed

    private void removeBtnClassifierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBtnClassifierActionPerformed
        m_Log.statusMessage("OK");
        
        if(classifierTable.getSelectedRow() != -1){
            listClassifier.remove(classifierTable.getSelectedRow());
            classifierTableModel.removeRow(classifierTable.getSelectedRow());
                        
            checkRun();
        }else{
            m_Log.logMessage("No row is selected");
            m_Log.statusMessage("See error log");
        }
    }//GEN-LAST:event_removeBtnClassifierActionPerformed

    private void datasetsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_datasetsTableMouseClicked
        Instances inst = listInstances.get(datasetsTable.getSelectedRow());
        String[] attribNames = new String[inst.numAttributes()];

        for (int i = 0; i < inst.numAttributes(); i++) {
            String type = "(" + Attribute.typeToStringShort(inst.attribute(i)) + ") ";
            String attnm = inst.attribute(i).name();
            attribNames[i] = type + attnm;
        }

        selectionClassCombo.setModel(new DefaultComboBoxModel(attribNames));
        
        if (inst.classIndex() < -1) {
            selectionClassCombo.setSelectedIndex(attribNames.length - 1);
        } else {
            selectionClassCombo.setSelectedIndex(inst.classIndex());
        }
        
        if(inst.classAttribute().isNominal()){
            String[] numValuesClass = new String[inst.attribute(inst.classIndex()).numValues()];
        
            for(int j = 0; j < numValuesClass.length; j++){
                numValuesClass[j] = inst.attribute(inst.classIndex()).value(j);
            }

            selectionPositiveClassCombo.setModel(new DefaultComboBoxModel(numValuesClass));

            selectionPositiveClassCombo.setSelectedIndex(listClassPositive.get(datasetsTable.getSelectedRow()));
        } else {
            selectionPositiveClassCombo.setModel(new DefaultComboBoxModel());
        }  
    }//GEN-LAST:event_datasetsTableMouseClicked

    private void selectionClassComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_selectionClassComboItemStateChanged
        Instances inst = listInstances.get(datasetsTable.getSelectedRow());

        inst.setClassIndex(selectionClassCombo.getSelectedIndex());

        datasetsTable.setValueAt(inst.attribute(selectionClassCombo.getSelectedIndex()).name(), datasetsTable.getSelectedRow(), 4);
    }//GEN-LAST:event_selectionClassComboItemStateChanged

    private void stopExpBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopExpBtnActionPerformed
        final SwingWorker worker2 = new SwingWorker(){
            @Override
            protected Object doInBackground() throws Exception {
                worker.cancel(true); // Disable SwingWorker that contains the pool
                executor.shutdown(); // Disable new tasks from being submitted
                
                try {
                    executor.shutdownNow(); // Cancel currently executing tasks
                    
                    // Wait a while for existing tasks to terminate
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow(); // Cancel currently executing tasks
                        
                        // Wait a while for tasks to respond to being cancelled
                        if (!executor.awaitTermination(60, TimeUnit.SECONDS)){
                            m_Log.logMessage("Pool did not terminate");
                        }
                    }
                } catch (InterruptedException ie) {
                    // (Re-)Cancel if current thread also interrupted
                    executor.shutdownNow();
                    // Preserve interrupt status
                    Thread.currentThread().interrupt();
                }

                stopExpBtn.setEnabled(false);
                runExpBtn.setEnabled(true);
                progressExp.setValue(0);
                return null;
            }	
        };
        
        worker2.execute();
    }//GEN-LAST:event_stopExpBtnActionPerformed

    private void runExpBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runExpBtnActionPerformed
        runExpBtn.setEnabled(false);    
        stopExpBtn.setEnabled(true);
        if (progressExp.getValue() != 0) {
            progressExp.setValue(0);
        } 

        //to avoid interface blocking
        worker = new SwingWorker(){
            @Override
            protected Object doInBackground() throws Exception {
                startAttributeSelectionExp();
                return null;
            }	
        };
        
        worker.execute();
    }//GEN-LAST:event_runExpBtnActionPerformed

    private void kappaMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_kappaMetricsCheckBoxMouseClicked
        if(kappaMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("Kappa", metricsTableModel, kappa());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("Kappa", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_kappaMetricsCheckBoxMouseClicked

    private void maeMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_maeMetricsCheckBoxMouseClicked
        if(maeMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("MAE", metricsTableModel, mae());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("MAE", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_maeMetricsCheckBoxMouseClicked

    private void precisionMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_precisionMetricsCheckBoxMouseClicked
        if(precisionMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("Precision", metricsTableModel, precision());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("Precision", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_precisionMetricsCheckBoxMouseClicked

    private void rmseMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rmseMetricsCheckBoxMouseClicked
        if(rmseMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("RMSE", metricsTableModel, rmse());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("RMSE", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_rmseMetricsCheckBoxMouseClicked

    private void r2MetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_r2MetricsCheckBoxMouseClicked
        if(r2MetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("R2", metricsTableModel, r2());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("R2", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_r2MetricsCheckBoxMouseClicked

    private void saveCSVPredictionsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCSVPredictionsBtnActionPerformed
        instPredictions = createObjectInstances(predictionsTableModel, "Predictions");
        saveCSVTable(instPredictions);
    }//GEN-LAST:event_saveCSVPredictionsBtnActionPerformed

    private void loadDatasetPredictionsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDatasetPredictionsBtnActionPerformed
        instPredictions = createObjectInstances(predictionsTableModel, "Predictions");
        getExplorer().getPreprocessPanel().setInstances(instPredictions);
    }//GEN-LAST:event_loadDatasetPredictionsBtnActionPerformed

    private void saveARFFPredictionsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveARFFPredictionsBtnActionPerformed
        instPredictions = createObjectInstances(predictionsTableModel, "Predictions");
        try {
            saveArffTable(instPredictions);
        } catch (Exception ex) {
            m_Log.logMessage(ex.getMessage());
        }
    }//GEN-LAST:event_saveARFFPredictionsBtnActionPerformed

    private void saveCSVMetricsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCSVMetricsBtnActionPerformed
        instMetrics = createObjectInstances(metricsTableModel, "Metrics");
        saveCSVTable(instMetrics);
    }//GEN-LAST:event_saveCSVMetricsBtnActionPerformed

    private void saveARFFMetricsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveARFFMetricsBtnActionPerformed
        instMetrics = createObjectInstances(metricsTableModel, "Metrics");
        try {
            saveArffTable(instMetrics);
        } catch (Exception ex) {
            m_Log.logMessage(ex.getMessage());
        }
    }//GEN-LAST:event_saveARFFMetricsBtnActionPerformed

    private void loadDatasetMetricsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDatasetMetricsBtnActionPerformed
        instMetrics = createObjectInstances(metricsTableModel, "Metrics");
        getExplorer().getPreprocessPanel().setInstances(instMetrics);
    }//GEN-LAST:event_loadDatasetMetricsBtnActionPerformed

    private void updatePredictionsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updatePredictionsBtnActionPerformed
        if(resultsAttrSelExp != null){
            List<String> datasetsSelect = datasetsList.getSelectedValuesList();
            List<String> evalsSelect = evaluatorList.getSelectedValuesList();
            List<String> searchsSelect = searchList.getSelectedValuesList();
            List<String> clsSelect = classifierList.getSelectedValuesList();
            List<ResultsAttrSelExp> objectSelected = new ArrayList<>();
            DefaultTableModel newTableModel = new DefaultTableModel();

            //clean columns of previous attributes if there were any
            if(predictionsTableModel.getColumnCount() > 6){
                predictionsTableModel.setColumnCount(6);
            }

            for(int cN = 0; cN < predictionsTableModel.getColumnCount(); cN++){
                newTableModel.addColumn(predictionsTableModel.getColumnName(cN));
            }

            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String dS = null;
                String eV = null;
                String search = null;
                String cls = null;

                try {
                    dS = objectResult.get().getInst().relationName();
                    cls = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));
                    eV = "original";
                    search = "original";

                    if(objectResult.get().getEvaluator() != null){
                        eV = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                        search = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));

                        if(dS != null && datasetsSelect.contains(dS)){
                            if(eV != null && evalsSelect.contains(eV) && search != null && searchsSelect.contains(search)){
                                if(cls != null && clsSelect.contains(cls)){
                                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                                    if(evalResult != null){
                                        if(evalResult.predictions() != null){
                                            objectSelected.add(objectResult.get());
                                            List<Prediction> valuesPredict = evalResult.predictions();

                                            for(int z = 0; z < valuesPredict.size(); z++){
                                                newTableModel.addRow(new Object[]{valuesPredict.get(z).actual(), valuesPredict.get(z).predicted(),
                                                dS, eV, search, cls});
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }else{
                        if(dS != null && datasetsSelect.contains(dS)){
                            if(evalsSelect.contains(eV) && searchsSelect.contains(search)){
                                if(cls != null && clsSelect.contains(cls)){
                                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                                    if(evalResult != null){
                                        if(evalResult.predictions() != null){
                                            objectSelected.add(objectResult.get());
                                            List<Prediction> valuesPredict = evalResult.predictions();

                                            for(int z = 0; z < valuesPredict.size(); z++){
                                                newTableModel.addRow(new Object[]{valuesPredict.get(z).actual(), valuesPredict.get(z).predicted(),
                                                dS, eV, search, cls});
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            predictionsTableModel = newTableModel;

            if(!attributesTextField.getText().trim().equals("") && !objectSelected.isEmpty()){
                String[] attrSelected = attributesTextField.getText().split(",");
                List data = new ArrayList();
                int indexAttrSel;

                for(int j = 0; j < attrSelected.length; j++){
                    try{
                        indexAttrSel = Integer.parseInt(attrSelected[j].trim())-1;

                        for(int z = 0; z < objectSelected.size(); z++){
                            Instances test = objectSelected.get(z).getTest();
                            
                            for(int t = 0; t < test.size(); t++){
                                if(test.get(t).attribute(indexAttrSel).isNumeric()){
                                    data.add(test.get(t).value(indexAttrSel));
                                } else {
                                    data.add(test.get(t).attribute(indexAttrSel).value((int)test.get(t).value(indexAttrSel)));
                                }
                            }
                        }

                        predictionsTableModel = addColumnTableModel("Attribute "+(indexAttrSel+1), predictionsTableModel, data);
                        data.clear();
                        m_Log.statusMessage("OK");
                    } catch(Exception ex){
                        m_Log.logMessage("Exception: " + ex.getMessage());
                        m_Log.statusMessage("See error log");
                    }  
                }
            }

            predictionsTable.setModel(predictionsTableModel);
        }
    }//GEN-LAST:event_updatePredictionsBtnActionPerformed

    private void selectionPositiveClassComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_selectionPositiveClassComboItemStateChanged
        Instances inst = listInstances.get(datasetsTable.getSelectedRow());
        
        listClassPositive.set(datasetsTable.getSelectedRow(), selectionPositiveClassCombo.getSelectedIndex());
        
        datasetsTable.setValueAt(inst.attribute(inst.classIndex()).value(selectionPositiveClassCombo.getSelectedIndex()), datasetsTable.getSelectedRow(), 5);
    }//GEN-LAST:event_selectionPositiveClassComboItemStateChanged

    private void accuracyMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_accuracyMetricsCheckBoxMouseClicked
        if(accuracyMetricsCheckBox.isSelected() == true){
            try {       
                metricsTableModel = addColumnTableModel("Accuracy", metricsTableModel, accuracy());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("Accuracy", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_accuracyMetricsCheckBoxMouseClicked

    private void recallMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_recallMetricsCheckBoxMouseClicked
        if(recallMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("Recall", metricsTableModel, recall());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("Recall", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_recallMetricsCheckBoxMouseClicked

    private void fMeasureMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fMeasureMetricsCheckBoxMouseClicked
        if(fMeasureMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("F-measure", metricsTableModel, fMeasure());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("F-measure", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_fMeasureMetricsCheckBoxMouseClicked

    private void mccMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mccMetricsCheckBoxMouseClicked
        if(mccMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("MCC", metricsTableModel, mcc());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("MCC", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_mccMetricsCheckBoxMouseClicked

    private void aucMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aucMetricsCheckBoxMouseClicked
        if(aucMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("AUC", metricsTableModel, auc());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("AUC", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_aucMetricsCheckBoxMouseClicked

    private void mseMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mseMetricsCheckBoxMouseClicked
        if(mseMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("MSE", metricsTableModel, mse());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("MSE", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_mseMetricsCheckBoxMouseClicked

    private void mapeMetricsCheckBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mapeMetricsCheckBoxMouseClicked
        if(mapeMetricsCheckBox.isSelected() == true){
            try {
                metricsTableModel = addColumnTableModel("MAPE", metricsTableModel, mape());
                metricsTable.setModel(metricsTableModel);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }else{
            metricsTableModel = removeColumnTableModel("MAPE", metricsTableModel);
            metricsTable.setModel(metricsTableModel);
        }
    }//GEN-LAST:event_mapeMetricsCheckBoxMouseClicked

    private void updateGraphBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateGraphBtnActionPerformed
        if(resultsAttrSelExp != null){
            if(!xAxis1ComboBox.getSelectedItem().equals(xAxis2ComboBox.getSelectedItem())){
                updateGraph();
            }else{
                m_Log.statusMessage("The value of the axis are same.");
            }
        }
    }//GEN-LAST:event_updateGraphBtnActionPerformed

    private void savePNGGraphBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePNGGraphBtnActionPerformed
        if(barChartPanel.getComponents().length != 0){
            savePNGGraph();
        }
    }//GEN-LAST:event_savePNGGraphBtnActionPerformed

    private void savePDFGraphBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePDFGraphBtnActionPerformed
        if(barChartPanel.getComponents().length != 0){
            savePDFGraph();
        }
    }//GEN-LAST:event_savePDFGraphBtnActionPerformed

    private void saveBBDDBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBBDDBtnActionPerformed
        saveBBDD();
    }//GEN-LAST:event_saveBBDDBtnActionPerformed

    private void loadBBDDBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBBDDBtnActionPerformed
        loadBBDD();
    }//GEN-LAST:event_loadBBDDBtnActionPerformed

    private void addFolderBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFolderBtnActionPerformed
        String initialDir = ExplorerDefaults.getInitialDirectory();
        ConverterFileChooser m_FileChooser = new ConverterFileChooser(new File(initialDir));
        Instances instances;
        m_FileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = m_FileChooser.showOpenDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
        
            File [] files = m_FileChooser.getSelectedFile().listFiles();
            for (File file : files) {
                try {
                    instances = DataSource.read(file.toString());
                    addInstancesToDatasetList(instances, 1);
                } catch (Exception ex) {
                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }            
        }
    }//GEN-LAST:event_addFolderBtnActionPerformed

    private void saveLatexMetricsBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveLatexMetricsBtnActionPerformed
        instMetrics = createObjectInstances(metricsTableModel, "Metrics");
        saveLatexTable(instMetrics, "Metrics");
    }//GEN-LAST:event_saveLatexMetricsBtnActionPerformed

    private void saveExpBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveExpBtnActionPerformed
        m_Log.statusMessage("Saving experiment. Please wait...");
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser dirChooser = new JFileChooser(new File(initialDir));
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int returnVal = dirChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            //Datasets
            File newDir = new File(dirChooser.getSelectedFile() + "/" + nameExpTextField.getText() + "/datasets/");
            
            try{
                newDir.mkdirs();
            }catch(Exception ex){
                m_Log.logMessage("Error creating directory");
                m_Log.statusMessage("See error log");
            }
            
            //Save domains
            for (int i = 0; i < listInstances.size(); i++) {
                try {
                    ConverterUtils.DataSink.write(newDir.getAbsolutePath() + "/" + listInstances.get(i).relationName() + ".arff", listInstances.get(i));
                } catch (Exception ex) {
                    m_Log.logMessage("Some domain can not be write in the specified directory");
                    m_Log.statusMessage("See error log");
                }
            }
            
            //Configuration file
            //Index class
            String indexClasses = "Index classes = ";
            
            for(int i = 0; i < listInstances.size(); i++){
                indexClasses += listInstances.get(i).classIndex() + ";";
            }
            
            //Positive class
            String positiveClasses = "Positive classes = ";
            
            for(int i = 0; i < listClassPositive.size(); i++){
                positiveClasses += listClassPositive.get(i) + ";";
            }
            
            //Evaluator
            String evaluator = "Evaluator = ";
            
            if(!listEvaluators.isEmpty()){
                for(int i = 0; i < listEvaluators.size(); i++){
                    evaluator += listEvaluators.get(i).getClass().getName() + " " + Utils.joinOptions(((OptionHandler)listEvaluators.get(i)).getOptions()) + ";";
                }
            }
            
            //Search
            String search = "Search = ";
            
            if(!listSearchAlgorithms.isEmpty()){
                for(int i = 0; i < listSearchAlgorithms.size(); i++){
                    search += listSearchAlgorithms.get(i).getClass().getName() + " " + Utils.joinOptions(((OptionHandler)listSearchAlgorithms.get(i)).getOptions()) + ";";
                }
            }
            
            //Classifier
            String classifier = "Classifier = ";
            
            if(!listClassifier.isEmpty()){
                for(int i = 0; i < listClassifier.size(); i++){
                    classifier += listClassifier.get(i).getClass().getName() + " " + Utils.joinOptions(((OptionHandler)listClassifier.get(i)).getOptions()) + ";";
                }
            }
            
            //Method of validation
            String validationMethod = "Validation = ";
            String validationConf = "Validation conf = ";
            
            if(holdOutSplitBtn.isSelected()){
                validationMethod += "1";
                validationConf += holdOutSplitTextField.getText();
                
                
                if(preserveOrderCheckbox.isSelected()){
                    validationConf += ";Yes";
                }else{
                    validationConf += ";No";
                }
            }else if(crossValidationBtn.isSelected()){
                validationMethod += "2";
                validationConf += crossValidationTextField.getText();
            }else if(leaveOneOutBtn.isSelected()){
                validationMethod += "3";
                validationConf += "";
            }
            
            //Threads
            String threads = "Threads = " + numThreadsTextField.getText();
            
            File conf = new File(dirChooser.getSelectedFile() + "/" + nameExpTextField.getText() +"/configuration.cfg");
            
            BufferedWriter bw;
            
            if(!conf.exists()){
                try {
                    bw = new BufferedWriter(new FileWriter(conf));
                    bw.write(indexClasses);
                    bw.newLine();
                    bw.write(positiveClasses);
                    bw.newLine();
                    bw.write(evaluator);
                    bw.newLine();
                    bw.write(search);
                    bw.newLine();
                    bw.write(classifier);
                    bw.newLine();
                    bw.write(validationMethod);
                    bw.newLine();
                    bw.write(validationConf);
                    bw.newLine();
                    bw.write(threads);
                    bw.close();
                    
                    m_Log.statusMessage("Experiment saved succesfully");
                } catch (IOException ex) {
                    m_Log.logMessage("Error creating configuration file");
                    m_Log.statusMessage("See error log");
                }
            }
        }
    }//GEN-LAST:event_saveExpBtnActionPerformed

    private void loadExpBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadExpBtnActionPerformed
        m_Log.statusMessage("Loading experiment. Please wait...");
        
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser dirChooser = new JFileChooser(new File(initialDir));
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int returnVal = dirChooser.showOpenDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = new File(dirChooser.getSelectedFile()+"/datasets/");
            
            //Load datasets
            File[] files = f.listFiles();

            Instances inst = null;
            
            listInstances = new ArrayList<>();
            
            datasetsTableModel.setRowCount(0);
            
            for (File file : files) {
                try {
                    inst = DataSource.read(file.toString());
                    inst.setClassIndex(inst.numAttributes()-1);
                    
                    datasetsTableModel.addRow(new Object[]{inst.relationName(), inst.numInstances(), inst.numAttributes() - 1, inst.numClasses(),
                        inst.attribute(inst.numAttributes() - 1).name(), inst.attribute(inst.classIndex()).value(0)});
                    
                    listInstances.add(inst);
                    
                    try{
                        listClassPositive.add(Integer.parseInt(inst.attribute(inst.classIndex()).value(0)));
                    }catch(NumberFormatException ex){
                        listClassPositive.add(0);
                    }
                    
                    m_Log.statusMessage("OK");
                } catch (Exception ex) {
                    m_Log.logMessage("Some ARFF file cannot be read");
                    m_Log.statusMessage("See error log");
                }
            }
            
            //Read cfg file
            File cfgFile = new File(dirChooser.getSelectedFile() + "/configuration.cfg");
            FileReader fr;
            String contentFile = "";
            
            try {
                fr = new FileReader(cfgFile);
                BufferedReader br = new BufferedReader(fr);
                
                String line;
                
                while((line = br.readLine()) != null){
                    contentFile += line + "\n";
                }
                    
                fr.close();     
            } catch (FileNotFoundException ex) {
                m_Log.logMessage("The configuration file could not found");
                m_Log.statusMessage("See error log");
            } catch (IOException ex) {
                m_Log.logMessage("The configuration file could not be read");
                m_Log.statusMessage("See error log");
            }
            
            String[] contentSplit = contentFile.split("\\n");
            
            String[] auxSplit = contentSplit[0].split(" = ");
            
            auxSplit = auxSplit[1].split(";");
            
            for(int i = 0; i < auxSplit.length; i++){
                listInstances.get(i).setClassIndex(Integer.parseInt(auxSplit[i]));
            }
            
            auxSplit = contentSplit[1].split(" = ");
            
            auxSplit = auxSplit[1].split(";");
            
            listClassPositive = new ArrayList<>();
            
            for(int i = 0; i < auxSplit.length; i++){
                listClassPositive.add(Integer.parseInt(auxSplit[i]));
            }
            
            
            //Evaluator and Search
            listEvaluators = new ArrayList<>();
            listSearchAlgorithms = new ArrayList<>();
            featuresTableModel.setRowCount(0);
            
            auxSplit = contentSplit[2].split(" = ");
            
            if(auxSplit.length > 1){
                String[] ev = auxSplit[1].split(";");
                String[] search = contentSplit[3].split(" = ")[1].split(";");
                
                try {
                    for(int i = 0; i < ev.length; i++){
                        String[] options = Utils.splitOptions(ev[i]);
                        String classname = options[0];
                        options[0] = "";
                        Class c = Utils.forName(Object.class, classname, null).getClass();

                        if (c.isArray()) {
                            Object[] arr = (Object[])Array.newInstance(c.getComponentType(), options.length - 1);

                            for (int j = 1; j < options.length; j++) {
                                String[] ops = Utils.splitOptions(options[j]);
                                String cname = ops[0];
                                ops[0] = "";
                                arr[j - 1] = Utils.forName(Object.class, cname, ops);
                            }
                            
                            m_AttributeEvaluatorEditor.setValue(arr);
                        } else {
                            m_AttributeEvaluatorEditor.setValue(Utils.forName(Object.class, classname, options));
                        }
                        
                        
                        listEvaluators.add(m_AttributeEvaluatorEditor.getValue());
                        
                        options = Utils.splitOptions(search[i]);
                        classname = options[0];
                        options[0] = "";
                        c = Utils.forName(Object.class, classname, null).getClass();

                        if (c.isArray()) {
                            Object[] arr = (Object[])Array.newInstance(c.getComponentType(), options.length - 1);

                            for (int j = 1; j < options.length; j++) {
                                String[] ops = Utils.splitOptions(options[j]);
                                String cname = ops[0];
                                ops[0] = "";
                                arr[j - 1] = Utils.forName(Object.class, cname, ops);
                            }
                            
                            m_AttributeSearchEditor.setValue(arr);
                        } else {
                            m_AttributeSearchEditor.setValue(Utils.forName(Object.class, classname, options));
                        }
                        
                        listSearchAlgorithms.add(m_AttributeSearchEditor.getValue());
                        
                        featuresTableModel.addRow(new Object[]{getSpec(m_AttributeEvaluatorEditor.getValue()), getSpec(m_AttributeSearchEditor.getValue())});
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            //Classifier
            listClassifier = new ArrayList<>();
            classifierTableModel.setRowCount(0);
            
            auxSplit = contentSplit[4].split(" = ");
            
            if(auxSplit.length > 1){
                String[] cls = auxSplit[1].split(";");
                
                try {
                    for(int i = 0; i < cls.length; i++){
                        String[] options = Utils.splitOptions(cls[i]);
                        String classname = options[0];
                        options[0] = "";
                        Class c = Utils.forName(Object.class, classname, null).getClass();

                        if (c.isArray()) {
                            Object[] arr = (Object[])Array.newInstance(c.getComponentType(), options.length - 1);

                            for (int j = 1; j < options.length; j++) {
                                String[] ops = Utils.splitOptions(options[j]);
                                String cname = ops[0];
                                ops[0] = "";
                                arr[j - 1] = Utils.forName(Object.class, cname, ops);
                            }
                            
                            m_ClassifierEditor.setValue(arr);
                        } else {
                            m_ClassifierEditor.setValue(Utils.forName(Object.class, classname, options));
                        }
                        
                        listClassifier.add(m_ClassifierEditor.getValue());
                        classifierTableModel.addRow(new Object[]{getSpec(m_ClassifierEditor.getValue())});
                    }
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            //Validation
            auxSplit = contentSplit[5].split(" = ");
            
            switch (auxSplit[1]) {
                case "1":
                    holdOutSplitBtn.setSelected(true);
                    holdOutSplitTextField.setEnabled(true);
                    auxSplit = contentSplit[6].split(" = ")[1].split(";");
                    holdOutSplitTextField.setText(auxSplit[0]);
                    
                    if(auxSplit[1].equals("No")){
                        preserveOrderCheckbox.setSelected(true);
                    }else{
                        preserveOrderCheckbox.setSelected(false);
                    }
                    
                    break;
                case "2":
                    crossValidationBtn.setSelected(true);
                    auxSplit = contentSplit[6].split(" = ");
                    crossValidationTextField.setEnabled(true);
                    crossValidationTextField.setText(auxSplit[1]);
                    
                    break;
                case "3":
                    leaveOneOutBtn.setSelected(true);
                    
                    break;
                default:
                    break;
            }
            
            updateRadioLinks();
                    
            //Threads
            auxSplit = contentSplit[7].split(" = ");
            
            numThreadsTextField.setText(auxSplit[1]);
        }    
    }//GEN-LAST:event_loadExpBtnActionPerformed

    private void metricStatisComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_metricStatisComboBoxItemStateChanged
        if(evt.getStateChange() == ItemEvent.SELECTED) {
            m_Log.logMessage("Perfoming statistical tests. Please wait...");
            m_Log.statusMessage("Perfoming statistical tests. Please wait...");
            messagesStatis.setText("Perfoming statistical tests. Please wait...");
            
            if(leaveOneOutBtn.isSelected()){
                try {
                    switch ((String)metricStatisComboBox.getSelectedItem()) {
                        case "F-measure":
                            bayesianComparator(fMeasureGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                            break;
                        case "Precision":
                            bayesianComparator(precisionGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                            break;
                        case "Recall":
                            bayesianComparator(recallGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                            break;
                        case "MAE":
                            bayesianComparator(maeGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                            break;
                        case "RMSE":
                            bayesianComparator(rmseGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                            break;
                        default:
                            break;
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else {
                try {
                    switch ((String)metricStatisComboBox.getSelectedItem()) {
                        case "F-measure":
                            bayesianComparator(fMeasureStatis());
                            break;
                        case "Accuracy":
                            bayesianComparator(accuracyStatis());
                            break;
                        case "Precision":
                            bayesianComparator(precisionStatis());
                            break;
                        case "Recall":
                            bayesianComparator(recallStatis());
                            break;
                        case "Kappa":
                            bayesianComparator(kappaStatis());
                            break;
                        case "MCC":
                            bayesianComparator(mccStatis());
                            break;
                        case "AUC":
                            bayesianComparator(aucStatis());
                            break;
                        case "MAE":
                            bayesianComparator(maeStatis());
                            break;
                        case "MSE":
                            bayesianComparator(mseStatis());
                            break;
                        case "RMSE":
                            bayesianComparator(rmseStatis());
                            break;
                        case "MAPE":
                            bayesianComparator(mapeStatis());
                            break;
                        case "R2":
                            bayesianComparator(r2Statis());
                            break;
                        default:
                            break;
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }//GEN-LAST:event_metricStatisComboBoxItemStateChanged

    private void filterStatisComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_filterStatisComboBoxItemStateChanged
        if(evt.getStateChange() == ItemEvent.SELECTED) {
            TableRowSorter<TableModel> modeloOrdenado = new TableRowSorter<TableModel>(statisticalTestTableModel);
            statisticalTestTable.setRowSorter(modeloOrdenado);

            switch ((String)filterStatisComboBox.getSelectedItem()) {
                case "Positives":
                    modeloOrdenado.setRowFilter(RowFilter.regexFilter("1|2", 9));
                    break;
                case "Negatives":
                    modeloOrdenado.setRowFilter(RowFilter.regexFilter("0", 9));
                    break;
                default:
                    modeloOrdenado.setRowFilter(RowFilter.regexFilter("0|1|2", 9));
                    break;
            }
        }
    }//GEN-LAST:event_filterStatisComboBoxItemStateChanged

    private void saveCSVStatisBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveCSVStatisBtnActionPerformed
        Instances inst = createObjectInstances(statisticalTestTableModel, "Statistical Test");
        saveCSVTable(inst);
    }//GEN-LAST:event_saveCSVStatisBtnActionPerformed

    private void putPreprocessStatisBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_putPreprocessStatisBtnActionPerformed
        Instances inst = createObjectInstances(statisticalTestTableModel, "Statistical Test");
        getExplorer().getPreprocessPanel().setInstances(inst);
    }//GEN-LAST:event_putPreprocessStatisBtnActionPerformed

    private void valueGroupsLOOTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_valueGroupsLOOTextFieldActionPerformed
        if(leaveOneOutBtn.isSelected()){
            m_Log.logMessage("Perfoming statistical tests. Please wait...");
            m_Log.statusMessage("Perfoming statistical tests. Please wait...");
            messagesStatis.setText("Perfoming statistical tests. Please wait...");
        
            if(statisticalTestTableModel.getRowCount() > 0){
                statisticalTestTableModel.setRowCount(0);
            }
            
            //to avoid interface blocking
            worker = new SwingWorker(){
                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        switch ((String)metricStatisComboBox.getSelectedItem()) {
                            case "F-measure":
                                bayesianComparator(fMeasureGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "Precision":
                                bayesianComparator(precisionGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "Recall":
                                bayesianComparator(recallGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "MAE":
                                bayesianComparator(maeGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "RMSE":
                                bayesianComparator(rmseGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            default:
                                break;
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    return null;
                }	
            };

            worker.execute();
        }else{
            JOptionPane.showMessageDialog(null, "Validation LOO has not been selected.");
        }
    }//GEN-LAST:event_valueGroupsLOOTextFieldActionPerformed

    private void ropeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ropeTextFieldActionPerformed
        m_Log.logMessage("Performing statistical tests. Please wait...");
        m_Log.statusMessage("Performing statistical tests. Please wait...");
        messagesStatis.setText("Performing statistical tests. Please wait...");
        
        if(statisticalTestTableModel.getRowCount() > 0){
            statisticalTestTableModel.setRowCount(0);
        }
        
        //to avoid interface blocking
        worker = new SwingWorker(){
            @Override
            protected Object doInBackground() throws Exception {
                if(leaveOneOutBtn.isSelected()){
                    try {
                        switch ((String)metricStatisComboBox.getSelectedItem()) {
                            case "F-measure":
                                bayesianComparator(fMeasureGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "Precision":
                                bayesianComparator(precisionGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "Recall":
                                bayesianComparator(recallGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "MAE":
                                bayesianComparator(maeGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            case "RMSE":
                                bayesianComparator(rmseGroupsLOO(Integer.parseInt(valueGroupsLOOTextField.getText())));
                                break;
                            default:
                                break;
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception ex) {
                        Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }else {
                    try {
                        switch ((String)metricStatisComboBox.getSelectedItem()) {
                            case "F-measure":
                                bayesianComparator(fMeasureStatis());
                                break;
                            case "Accuracy":
                                bayesianComparator(accuracyStatis());
                                break;
                            case "Precision":
                                bayesianComparator(precisionStatis());
                                break;
                            case "Recall":
                                bayesianComparator(recallStatis());
                                break;
                            case "Kappa":
                                bayesianComparator(kappaStatis());
                                break;
                            case "MCC":
                                bayesianComparator(mccStatis());
                                break;
                            case "AUC":
                                bayesianComparator(aucStatis());
                                break;
                            case "MAE":
                                bayesianComparator(maeStatis());
                                break;
                            case "MSE":
                                bayesianComparator(mseStatis());
                                break;
                            case "RMSE":
                                bayesianComparator(rmseStatis());
                                break;
                            case "MAPE":
                                bayesianComparator(mapeStatis());
                                break;
                            case "R2":
                                bayesianComparator(r2Statis());
                                break;
                            default:
                                break;
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                return null;
            }	
        };
        
        worker.execute();
    }//GEN-LAST:event_ropeTextFieldActionPerformed

    private void xAxis1ComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_xAxis1ComboBoxItemStateChanged
        updateFilters();
    }//GEN-LAST:event_xAxis1ComboBoxItemStateChanged

    private void xAxis2ComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_xAxis2ComboBoxItemStateChanged
        updateFilters();;
    }//GEN-LAST:event_xAxis2ComboBoxItemStateChanged
    private void updateFilters(){
        String[] dataComboBox;    
        
        if(xAxis1ComboBox.getSelectedItem().equals("Dataset") && xAxis2ComboBox.getSelectedItem().equals("Evaluator")){
            dataComboBox = new String[captionCL.size()];
        
            for(int i = 0; i < listClassifier.size(); i++){
                dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Classifier:");

            dataComboBox = new String[captionSE.size()];
        
            for(int i = 0; i < listSearchAlgorithms.size(); i++){
                dataComboBox[i] = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Search:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Dataset") && xAxis2ComboBox.getSelectedItem().equals("Search")){
            dataComboBox = new String[captionCL.size()];
        
            for(int i = 0; i < listClassifier.size(); i++){
                dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Classifier:");

            dataComboBox = new String[captionEV.size()];
            int i = 0;
            
            while(((String)captionTable.getValueAt(i, 0)).contains("E")){
                dataComboBox[i] = (String)captionTable.getValueAt(i, 0);
                i++;
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Evaluator:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Dataset") && xAxis2ComboBox.getSelectedItem().equals("Classifier")){
            dataComboBox = new String[captionEV.size()+1];
        
            int i = 0;
            
            while(((String)captionTable.getValueAt(i, 0)).contains("E")){
                dataComboBox[i] = (String)captionTable.getValueAt(i, 0);
                i++;
            }

            dataComboBox[dataComboBox.length-1] = "original";
            
            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Evaluator:");

            dataComboBox = new String[captionSE.size()+1];
        
            for(i = 0; i < listSearchAlgorithms.size(); i++){
                dataComboBox[i] = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));
            }

            dataComboBox[dataComboBox.length-1] = "original";
            
            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Search:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Evaluator") && xAxis2ComboBox.getSelectedItem().equals("Dataset")){
            dataComboBox = new String[captionCL.size()];
        
            for(int i = 0; i < listClassifier.size(); i++){
                dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Classifier:");

            dataComboBox = new String[captionSE.size()];
        
            for(int i = 0; i < listSearchAlgorithms.size(); i++){
                dataComboBox[i] = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Search:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Evaluator") && xAxis2ComboBox.getSelectedItem().equals("Search")){
            dataComboBox = new String[captionCL.size()];
        
            for(int i = 0; i < listClassifier.size(); i++){
                dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Classifier:");

            dataComboBox = new String[listInstances.size()];

            for(int i = 0; i < listInstances.size(); i++){
                dataComboBox[i] = listInstances.get(i).relationName();
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Dataset:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Evaluator") && xAxis2ComboBox.getSelectedItem().equals("Classifier")){
            dataComboBox = new String[captionSE.size()];
        
            for(int i = 0; i < listSearchAlgorithms.size(); i++){
                dataComboBox[i] = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Search:");

            dataComboBox = new String[listInstances.size()];

            for(int i = 0; i < listInstances.size(); i++){
                dataComboBox[i] = listInstances.get(i).relationName();
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Dataset:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Search") && xAxis2ComboBox.getSelectedItem().equals("Dataset")){
            dataComboBox = new String[captionCL.size()];
        
            for(int i = 0; i < listClassifier.size(); i++){
                dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Classifier:");

            dataComboBox = new String[captionEV.size()];
        
            int i = 0;
            
            while(((String)captionTable.getValueAt(i, 0)).contains("E")){
                dataComboBox[i] = (String)captionTable.getValueAt(i, 0);
                i++;
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Evaluator:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Search") && xAxis2ComboBox.getSelectedItem().equals("Evaluator")){
            dataComboBox = new String[captionCL.size()];
        
            for(int i = 0; i < listClassifier.size(); i++){
                dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Classifier:");

            dataComboBox = new String[listInstances.size()];

            for(int i = 0; i < listInstances.size(); i++){
                dataComboBox[i] = listInstances.get(i).relationName();
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Dataset:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Search") && xAxis2ComboBox.getSelectedItem().equals("Classifier")){
            dataComboBox = new String[captionEV.size()];
        
            int i = 0;
            
            while(((String)captionTable.getValueAt(i, 0)).contains("E")){
                dataComboBox[i] = (String)captionTable.getValueAt(i, 0);
                i++;
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Evaluator:");

            dataComboBox = new String[listInstances.size()];

            for(i = 0; i < listInstances.size(); i++){
                dataComboBox[i] = listInstances.get(i).relationName();
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Dataset:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Classifier") && xAxis2ComboBox.getSelectedItem().equals("Dataset")){
            dataComboBox = new String[captionEV.size()+1];
        
            int i = 0;
            
            while(((String)captionTable.getValueAt(i, 0)).contains("E")){
                dataComboBox[i] = (String)captionTable.getValueAt(i, 0);
                i++;
            }

            dataComboBox[dataComboBox.length-1] = "original";
            
            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Evaluator:");

            dataComboBox = new String[captionSE.size()+1];
        
            for(i = 0; i < listSearchAlgorithms.size(); i++){
                dataComboBox[i] = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));
            }
            
            dataComboBox[dataComboBox.length-1] = "original";

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Search:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Classifier") && xAxis2ComboBox.getSelectedItem().equals("Search")){
            dataComboBox = new String[captionEV.size()];
        
            int i = 0;
            
            while(((String)captionTable.getValueAt(i, 0)).contains("E")){
                dataComboBox[i] = (String)captionTable.getValueAt(i, 0);
                i++;
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Evaluator:");

            dataComboBox = new String[listInstances.size()];

            for(i = 0; i < listInstances.size(); i++){
                dataComboBox[i] = listInstances.get(i).relationName();
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Dataset:");
        }else if(xAxis1ComboBox.getSelectedItem().equals("Classifier") && xAxis2ComboBox.getSelectedItem().equals("Evaluator")){
            dataComboBox = new String[captionSE.size()];
        
            for(int i = 0; i < listSearchAlgorithms.size(); i++){
                dataComboBox[i] = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));
            }

            filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter1.setText("Search:");

            dataComboBox = new String[listInstances.size()];

            for(int i = 0; i < listInstances.size(); i++){
                dataComboBox[i] = listInstances.get(i).relationName();
            }

            filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
            labelFilter2.setText("Dataset:");
        }
    }
    
    private void updateGraph(){
        DefaultCategoryDataset result = new DefaultCategoryDataset();
        String metricSelected = (String)metricGraphComboBox.getSelectedItem();
        String xAxis1 = (String)xAxis1ComboBox.getSelectedItem();
        String xAxis2 = (String)xAxis2ComboBox.getSelectedItem();
        String f1 = labelFilter1.getText() + (String)filterGraph1ComboBox.getSelectedItem();
        String f2 = labelFilter2.getText() + (String)filterGraph2ComboBox.getSelectedItem();
        List dataMetrics = new ArrayList();
        int indexColF1 = 0;
        int indexColF2 = 0;
        
        if(f1.split(":")[0].equals("Classifier")){
            indexColF1 = 3;
        }else if(f1.split(":")[0].equals("Evaluator")){
            indexColF1 = 1;
        }else if(f1.split(":")[0].equals("Search")){
            indexColF1 = 2;
        }
        
        if(f2.split(":")[0].equals("Dataset")){
            indexColF2 = 0;
        }else if(f2.split(":")[0].equals("Evaluator")){
            indexColF2 = 1;
        }else if(f2.split(":")[0].equals("Search")){
            indexColF2 = 2;
        }

        f1 = f1.split(":")[1];
        f2 = f2.split(":")[1];
                
        try {
            switch (metricSelected) {
                case "Accuracy":
                    dataMetrics = accuracy();
                    break;
                case "Precision":
                    dataMetrics = precision();
                    break;
                case "Recall":
                    dataMetrics = recall();
                    break;
                case "F-measure":
                    dataMetrics = fMeasure();
                    break;
                case "Kappa":
                    dataMetrics = kappa();
                    break;
                case "MCC":
                    dataMetrics = mcc();
                    break;
                case "AUC":
                    dataMetrics = auc();
                    break;
                case "MAE":
                    dataMetrics = mae();
                    break;
                case "MSE":
                    dataMetrics = mse();
                    break;
                case "RMSE":
                    dataMetrics = rmse();
                    break;
                case "MAPE":
                    dataMetrics = mape();
                    break;
                case "R2":
                    dataMetrics = r2();
                    break;
                default:
                    break;
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(f1.equals("original") || f2.equals("original")){
            f1 = "original";
            filterGraph1ComboBox.setSelectedItem("original");
            f2 = "original";
            filterGraph2ComboBox.setSelectedItem("original");
        }

        for(int i = 0; i < dataMetrics.size(); i++){
            //if not applicable or an error has occurred
            if(dataMetrics.get(i) != null){
                try {
                    if((metricsTable.getValueAt(i, indexColF1).equals(f1) && metricsTable.getValueAt(i, indexColF2).equals(f2)) || 
                            (metricsTable.getValueAt(i, 3).equals(f1) && metricsTable.getValueAt(i, 1).equals("original") && indexColF1 != 1 && indexColF2 != 2) ||
                            (metricsTable.getValueAt(i, 1).equals("original") && metricsTable.getValueAt(i, 2).equals("original") && indexColF1 != 1 && indexColF2 != 2)){
                        if(xAxis1.equals("Dataset") && xAxis2.equals("Search")){
                                result.setValue((double)dataMetrics.get(i), 
                                    (String)metricsTableModel.getValueAt(i, 2), 
                                    (String)metricsTableModel.getValueAt(i, 0));
                        } else if(xAxis1.equals("Dataset") && xAxis2.equals("Evaluator")){
                                result.setValue((double)dataMetrics.get(i), 
                                    (String)metricsTableModel.getValueAt(i, 1), 
                                    (String)metricsTableModel.getValueAt(i, 0));
                        } else if(xAxis1.equals("Dataset") && xAxis2.equals("Classifier")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 3), 
                                (String)metricsTableModel.getValueAt(i, 0));
                        } else if(xAxis1.equals("Search") && xAxis2.equals("Dataset")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 0),
                                (String)metricsTableModel.getValueAt(i, 2));
                        } else if(xAxis1.equals("Search") && xAxis2.equals("Evaluator")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 1),
                                (String)metricsTableModel.getValueAt(i, 2));
                        } else if(xAxis1.equals("Search") && xAxis2.equals("Classifier")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 3),
                                (String)metricsTableModel.getValueAt(i, 2));
                        } else if(xAxis1.equals("Evaluator") && xAxis2.equals("Dataset")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 0),
                                (String)metricsTableModel.getValueAt(i, 1));
                        } else if(xAxis1.equals("Evaluator") && xAxis2.equals("Search")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 2),
                                (String)metricsTableModel.getValueAt(i, 1));
                        } else if(xAxis1.equals("Evaluator") && xAxis2.equals("Classifier")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 3),
                                (String)metricsTableModel.getValueAt(i, 1));
                        } else if(xAxis1.equals("Classifier") && xAxis2.equals("Dataset")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 0),
                                (String)metricsTableModel.getValueAt(i, 3));
                        } else if(xAxis1.equals("Classifier") && xAxis2.equals("Evaluator")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 1),
                                (String)metricsTableModel.getValueAt(i, 3));
                        } else if(xAxis1.equals("Classifier") && xAxis2.equals("Search")){
                            result.setValue((double)dataMetrics.get(i), 
                                (String)metricsTableModel.getValueAt(i, 2),
                                (String)metricsTableModel.getValueAt(i, 3)); 
                        }   
                    }
                } catch (Exception ex) {
                    result.setValue(0, "", "");
                }
            }
        }

        chart = ChartFactory.createBarChart(null, xAxis1+"/"+xAxis2, metricSelected,result, PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundAlpha(0.5f);
        ChartPanel chartPanel = new ChartPanel(chart);
        barChartPanel.removeAll();
        barChartPanel.setLayout(new java.awt.BorderLayout());
        barChartPanel.add(chartPanel);   
        barChartPanel.validate();
    }
    
    private void bayesianComparator(List metrics){
        try {
            String contentFile = convertListMetricsToString(metrics);
            /*Path path = Paths.get("");
            String directoryName = path.toAbsolutePath().normalize().toString();*/
            String tmpdir = System.getProperty("java.io.tmpdir");
            File oldConf = new File(tmpdir + "metricsJava.txt");
            oldConf.delete();
            File conf = new File(tmpdir + "metricsJava.txt");
            conf.setExecutable(true);
            conf.setReadable(true);
            conf.setWritable(true);
            
            BufferedWriter bw;
            
            if(!conf.exists()){
                try {
                    bw = new BufferedWriter(new FileWriter(conf));
                    bw.write(contentFile, 0, contentFile.length());
                    bw.close();
                } catch (IOException ex) {
                    m_Log.logMessage("Error creating metrics Java file");
                    m_Log.logMessage("" + ex.getMessage());
                    m_Log.logMessage("" + tmpdir);
                    m_Log.statusMessage("See error log");
                }
            }
            
            String dir = System.getProperty("user.home");
            String fileSeparator = File.separator;
            String[] bayComp;
            
            if(fileSeparator.equals("\\")){
                bayComp = new String[]{"python", dir + "\\wekafiles\\packages\\FS-Studio\\bayesianComparator.py", tmpdir, ropeTextField.getText()};
                //bayComp = new String[]{"python", "bayesianComparator.py", tmpdir, ropeTextField.getText()};
            }else{
                bayComp = new String[]{"python", dir + "/wekafiles/packages/FS-Studio/bayesianComparator.py", tmpdir, ropeTextField.getText()};
            }
              
            Process proc = Runtime.getRuntime().exec(bayComp);
            
            proc.waitFor();
            
            File file = new File(tmpdir + "metricsPython.txt");
            List<String> resultsBuff = new ArrayList();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                resultsBuff.add(line);
            }
            
            br.close();
            
            if(statisticalTestTableModel.getRowCount() > 0){
                statisticalTestTableModel.setRowCount(0);
            }
            
            List<String> com = new ArrayList<>();
            
            for(int i = 0; i < listEvaluators.size(); i++){
                String ev = findAlgorithms(captionEV, getSpec(listEvaluators.get(i)));
                String se = findAlgorithms(captionSE, getSpec(listSearchAlgorithms.get(i)));

                for(int j = 0; j < listClassifier.size(); j++){
                    String c = findAlgorithms(captionCL, getSpec(listClassifier.get(j)));

                    com.add(ev + " " + se + " " + c);
                }
            }
            
            String ev = "original";
            String se = "original";
            
            for(int i = 0; i < listClassifier.size(); i++){
                String c = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));

                com.add(ev + " " + se + " " + c);
            }
            
            List<String> auxCom = new ArrayList<>(); 
            
            for(int i = 0; i < com.size(); i++){
                for(int j = 0; j < com.size(); j++){
                    if(i != j){
                        auxCom.add(com.get(i) + ";" + com.get(j));
                    }
                }
            }
            
            for(int i = 0; i < auxCom.size(); i++){
                String[] aux = auxCom.get(i).split(";");
                String[] aux1 = aux[0].split(" ");
                String[] aux2 = aux[1].split(" ");
                String[] metric = resultsBuff.get(i).replace("[" , "").replace("]", "").split(" ");
                String[] auxMetric = new String[3];
                int indexM = 0;
                List<Double> max = new ArrayList<>();

                for(int n = 0; n < metric.length; n++){
                    if(!metric[n].equals("")){
                        max.add(Double.parseDouble(metric[n]));
                        auxMetric[indexM] = metric[n];
                        indexM++;
                    }
                }
                
                Collections.sort(max);

                if(max.get(max.size()-1).equals(Double.parseDouble(auxMetric[0]))){
                    statisticalTestTableModel.addRow(new Object[]{aux1[0], aux1[1], aux1[2], aux2[0], aux2[1], aux2[2], Double.parseDouble(auxMetric[0]), Double.parseDouble(auxMetric[1]), 
                        Double.parseDouble(auxMetric[2]), "1"});
                }else if(max.get(max.size()-1).equals(Double.parseDouble(auxMetric[1]))){
                    statisticalTestTableModel.addRow(new Object[]{aux1[0], aux1[1], aux1[2], aux2[0], aux2[1], aux2[2], Double.parseDouble(auxMetric[0]), Double.parseDouble(auxMetric[1]), 
                        Double.parseDouble(auxMetric[2]), "0"});
                }else{
                    statisticalTestTableModel.addRow(new Object[]{aux1[0], aux1[1], aux1[2], aux2[0], aux2[1], aux2[2], Double.parseDouble(auxMetric[0]), Double.parseDouble(auxMetric[1]), 
                        Double.parseDouble(auxMetric[2]), "2"});
                }
            }
            
            m_Log.statusMessage("Statistical tests results already produced.");
            m_Log.logMessage("Statistical tests results already produced.");
            messagesStatis.setText("Statistical tests results already produced.");
        } catch (IOException | InterruptedException ex) {  
            m_Log.statusMessage("Statistical tests could not be calculated, missing python, or numpy or baycomp packages.");
        }
    }
    
    private String convertListMetricsToString(List m){
        String s = "";
        
        for(int i = 0; i < m.size(); i++){
            s += m.get(i) + "\n";
        }
        
        return s;
    }
    
    private List fMeasureStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.fMeasure(listClassPositive.get(index)))){
                                    s += 0.0 + ";";
                                }else{
                                    s += Double.toString(evalResult.fMeasure(listClassPositive.get(index))*100) + ";";
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int index = foundInstances(objectResult.get().getInst());
                            
                            if(Double.isNaN(evalResult.fMeasure(listClassPositive.get(index)))){
                                s += 0.0 + ";";
                            }else{
                                s += Double.toString(evalResult.fMeasure(listClassPositive.get(index))*100) + ";";
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List accuracyStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                if(Double.isNaN(evalResult.pctCorrect())){
                                    s += 0.0 + ";";
                                }else{
                                    s += Double.toString(evalResult.pctCorrect()) + ";";
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            if(Double.isNaN(evalResult.pctCorrect())){
                                s += 0.0 + ";";
                            }else{
                                s += Double.toString(evalResult.pctCorrect()) + ";";
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List precisionStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.precision(listClassPositive.get(index)))){
                                    s += 0.0 + ";";
                                }else{
                                    s += Double.toString(evalResult.precision(listClassPositive.get(index))*100) + ";";
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int index = foundInstances(objectResult.get().getInst());
                                
                            if(Double.isNaN(evalResult.precision(listClassPositive.get(index)))){
                                s += 0.0 + ";";
                            }else{
                                s += Double.toString(evalResult.precision(listClassPositive.get(index))*100) + ";";
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List recallStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.recall(listClassPositive.get(index)))){
                                    s += 0.0 + ";";
                                }else{
                                    s += Double.toString(evalResult.recall(listClassPositive.get(index))*100) + ";";
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int index = foundInstances(objectResult.get().getInst());
                                
                            if(Double.isNaN(evalResult.recall(listClassPositive.get(index)))){
                                s += 0.0 + ";";
                            }else{
                                s += Double.toString(evalResult.recall(listClassPositive.get(index))*100) + ";";
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List kappaStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                s += Double.toString(evalResult.kappa()*100) + ";";
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            s += Double.toString(evalResult.kappa()*100) + ";";
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List mccStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.matthewsCorrelationCoefficient(listClassPositive.get(index)))){
                                    s += 0.0 + ";";
                                }else{
                                    s += Double.toString(evalResult.matthewsCorrelationCoefficient(listClassPositive.get(index))*100) + ";";
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int index = foundInstances(objectResult.get().getInst());
                                
                            if(Double.isNaN(evalResult.matthewsCorrelationCoefficient(listClassPositive.get(index)))){
                                s += 0.0 + ";";
                            }else{
                                s += Double.toString(evalResult.matthewsCorrelationCoefficient(listClassPositive.get(index))*100) + ";";
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List aucStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.areaUnderROC(listClassPositive.get(index)))){
                                    s += 0.0 + ";";
                                }else{
                                    s += Double.toString(evalResult.areaUnderROC(listClassPositive.get(index))*100) + ";";
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int index = foundInstances(objectResult.get().getInst());
                                
                            if(Double.isNaN(evalResult.areaUnderROC(listClassPositive.get(index)))){
                                s += 0.0 + ";";
                            }else{
                                s += Double.toString(evalResult.areaUnderROC(listClassPositive.get(index))*100) + ";";
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List maeStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                s += Double.toString(evalResult.meanAbsoluteError()*100) + ";";
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            s += Double.toString(evalResult.meanAbsoluteError()*100) + ";";
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List mseStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                s += Double.toString(Math.pow(evalResult.rootMeanSquaredError(), 2)) + ";";
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            s += Double.toString(Math.pow(evalResult.rootMeanSquaredError(), 2)) + ";";
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List rmseStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                s += Double.toString(evalResult.rootMeanSquaredError()) + ";";
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            s += Double.toString(evalResult.rootMeanSquaredError()) + ";";
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List mapeStatis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        int size = 0;
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                double aux = 0.0;

                                List<Prediction> valuesPredict = evalResult.predictions();

                                for(int e = 0; e < valuesPredict.size(); e++){
                                    aux += Math.abs(valuesPredict.get(e).actual() - valuesPredict.get(e).predicted())/valuesPredict.get(e).predicted();
                                    size++;
                                }

                                s += Double.toString((aux/size)*100) + ";";
                                size = 0;
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            double aux = 0.0;

                            List<Prediction> valuesPredict = evalResult.predictions();

                            for(int e = 0; e < valuesPredict.size(); e++){
                                aux += Math.abs(valuesPredict.get(e).actual() - valuesPredict.get(e).predicted())/valuesPredict.get(e).predicted();
                                size++;
                            }

                            s += Double.toString((aux/size)*100) + ";";
                            size = 0;
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List r2Statis() throws InterruptedException, ExecutionException{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                try {
                                    s += Double.toString(evalResult.correlationCoefficient()) + ";";
                                } catch (Exception ex) {
                                    s += "0.0;";
                                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            try {
                                s += Double.toString(evalResult.correlationCoefficient()) + ";";
                            } catch (Exception ex) {
                                s += "0.0;";
                                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List fMeasureGroupsLOO(int groups) throws InterruptedException, ExecutionException, Exception{
        List m = new ArrayList();
        String s = "";
        ConfusionMatrix confM;
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int indexPos = foundInstances(objectResult.get().getInst());
                                List<Prediction> valuesPredict = evalResult.predictions();
                                List<Prediction> aux = new ArrayList<>();
                                int last = 0;
                                int cont = objectResult.get().getInst().numInstances()/groups;
                                int index = objectResult.get().getInst().classIndex();
                                String[] classNames = new String[objectResult.get().getInst().attribute(index).numValues()];
                                
                                for(int value = 0; value < objectResult.get().getInst().attribute(index).numValues(); value++){
                                    classNames[value] = objectResult.get().getInst().attribute(index).value(value);
                                }

                                for(int rest = 0; rest < cont; rest++){
                                    if((last+groups) < valuesPredict.size()){
                                        for(int g = last; g < (last+groups); g++){
                                            aux.add(valuesPredict.get(g));
                                        }

                                        confM = new ConfusionMatrix(classNames);
                                        
                                        confM.addPredictions((ArrayList<Prediction>) aux);
                                        
                                        if(Double.isNaN(confM.getTwoClassStats(indexPos).getFMeasure())){
                                            s += "0.0;";
                                        }else{
                                            s += confM.getTwoClassStats(indexPos).getFMeasure() + ";";
                                        }
                                        
                                        aux.clear();
                                        last += groups;
                                    }
                                }
                                
                                if(last < valuesPredict.size()){
                                    for(int g = last; g < valuesPredict.size(); g++){
                                        aux.add(valuesPredict.get(g));
                                    }
                                    
                                    confM = new ConfusionMatrix(classNames);
                                    
                                    confM.addPredictions((ArrayList<Prediction>) aux);
                                    
                                    if(Double.isNaN(confM.getTwoClassStats(indexPos).getFMeasure())){
                                        s += "0.0;";
                                    }else{
                                        s += confM.getTwoClassStats(indexPos).getFMeasure() + ";";
                                    }
                                    
                                    aux.clear();
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int indexPos = foundInstances(objectResult.get().getInst());
                            List<Prediction> valuesPredict = evalResult.predictions();
                            List<Prediction> aux = new ArrayList<>();
                            int last = 0;
                            int cont = objectResult.get().getInst().numInstances()/groups;
                            int index = objectResult.get().getInst().classIndex();
                            String[] classNames = new String[objectResult.get().getInst().attribute(index).numValues()];

                            for(int value = 0; value < objectResult.get().getInst().attribute(index).numValues(); value++){
                                classNames[value] = objectResult.get().getInst().attribute(index).value(value);
                            }

                            for(int rest = 0; rest < cont; rest++){
                                if((last+groups) < valuesPredict.size()){
                                    for(int g = last; g < (last+groups); g++){
                                        aux.add(valuesPredict.get(g));
                                    }

                                    confM = new ConfusionMatrix(classNames);

                                    confM.addPredictions((ArrayList<Prediction>) aux);

                                    if(Double.isNaN(confM.getTwoClassStats(indexPos).getFMeasure())){
                                        s += "0.0;";
                                    }else{
                                        s += confM.getTwoClassStats(indexPos).getFMeasure() + ";";
                                    }

                                    aux.clear();
                                    last += groups;
                                }
                            }

                            if(last < valuesPredict.size()){
                                for(int g = last; g < valuesPredict.size(); g++){
                                    aux.add(valuesPredict.get(g));
                                }

                                confM = new ConfusionMatrix(classNames);

                                confM.addPredictions((ArrayList<Prediction>) aux);

                                if(Double.isNaN(confM.getTwoClassStats(indexPos).getFMeasure())){
                                    s += "0.0;";
                                }else{
                                    s += confM.getTwoClassStats(indexPos).getFMeasure() + ";";
                                }

                                aux.clear();
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List precisionGroupsLOO(int groups) throws InterruptedException, ExecutionException, Exception{
        List m = new ArrayList();
        String s = "";
        ConfusionMatrix confM;
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int indexPos = foundInstances(objectResult.get().getInst());
                                List<Prediction> valuesPredict = evalResult.predictions();
                                List<Prediction> aux = new ArrayList<>();
                                int last = 0;
                                int cont = objectResult.get().getInst().numInstances()/groups;
                                int index = objectResult.get().getInst().classIndex();
                                String[] classNames = new String[objectResult.get().getInst().attribute(index).numValues()];
                                
                                for(int value = 0; value < objectResult.get().getInst().attribute(index).numValues(); value++){
                                    classNames[value] = objectResult.get().getInst().attribute(index).value(value);
                                }

                                for(int rest = 0; rest < cont; rest++){
                                    if((last+groups) < valuesPredict.size()){
                                        for(int g = last; g < (last+groups); g++){
                                            aux.add(valuesPredict.get(g));
                                        }

                                        confM = new ConfusionMatrix(classNames);
                                        
                                        confM.addPredictions((ArrayList<Prediction>) aux);
                                        
                                        if(Double.isNaN(confM.getTwoClassStats(indexPos).getPrecision())){
                                            s += "0.0;";
                                        }else{
                                            s += confM.getTwoClassStats(indexPos).getPrecision() + ";";
                                        }
                                        
                                        aux.clear();
                                        last += groups;
                                    }
                                }
                                
                                if(last < valuesPredict.size()){
                                    for(int g = last; g < valuesPredict.size(); g++){
                                        aux.add(valuesPredict.get(g));
                                    }
                                    
                                    confM = new ConfusionMatrix(classNames);
                                    
                                    confM.addPredictions((ArrayList<Prediction>) aux);
                                    
                                    if(Double.isNaN(confM.getTwoClassStats(indexPos).getPrecision())){
                                        s += "0.0;";
                                    }else{
                                        s += confM.getTwoClassStats(indexPos).getPrecision() + ";";
                                    }
                                    
                                    aux.clear();
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int indexPos = foundInstances(objectResult.get().getInst());
                            List<Prediction> valuesPredict = evalResult.predictions();
                            List<Prediction> aux = new ArrayList<>();
                            int last = 0;
                            int cont = objectResult.get().getInst().numInstances()/groups;
                            int index = objectResult.get().getInst().classIndex();
                            String[] classNames = new String[objectResult.get().getInst().attribute(index).numValues()];

                            for(int value = 0; value < objectResult.get().getInst().attribute(index).numValues(); value++){
                                classNames[value] = objectResult.get().getInst().attribute(index).value(value);
                            }

                            for(int rest = 0; rest < cont; rest++){
                                if((last+groups) < valuesPredict.size()){
                                    for(int g = last; g < (last+groups); g++){
                                        aux.add(valuesPredict.get(g));
                                    }

                                    confM = new ConfusionMatrix(classNames);

                                    confM.addPredictions((ArrayList<Prediction>) aux);

                                    if(Double.isNaN(confM.getTwoClassStats(indexPos).getPrecision())){
                                        s += "0.0;";
                                    }else{
                                        s += confM.getTwoClassStats(indexPos).getPrecision() + ";";
                                    }

                                    aux.clear();
                                    last += groups;
                                }
                            }

                            if(last < valuesPredict.size()){
                                for(int g = last; g < valuesPredict.size(); g++){
                                    aux.add(valuesPredict.get(g));
                                }

                                confM = new ConfusionMatrix(classNames);

                                confM.addPredictions((ArrayList<Prediction>) aux);

                                if(Double.isNaN(confM.getTwoClassStats(indexPos).getPrecision())){
                                    s += "0.0;";
                                }else{
                                    s += confM.getTwoClassStats(indexPos).getPrecision() + ";";
                                }

                                aux.clear();
                                }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List recallGroupsLOO(int groups) throws InterruptedException, ExecutionException, Exception{
        List m = new ArrayList();
        String s = "";
        ConfusionMatrix confM;
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                int indexPos = foundInstances(objectResult.get().getInst());
                                List<Prediction> valuesPredict = evalResult.predictions();
                                List<Prediction> aux = new ArrayList<>();
                                int last = 0;
                                int cont = objectResult.get().getInst().numInstances()/groups;
                                int index = objectResult.get().getInst().classIndex();
                                String[] classNames = new String[objectResult.get().getInst().attribute(index).numValues()];
                                
                                for(int value = 0; value < objectResult.get().getInst().attribute(index).numValues(); value++){
                                    classNames[value] = objectResult.get().getInst().attribute(index).value(value);
                                }

                                for(int rest = 0; rest < cont; rest++){
                                    if((last+groups) < valuesPredict.size()){
                                        for(int g = last; g < (last+groups); g++){
                                            aux.add(valuesPredict.get(g));
                                        }

                                        confM = new ConfusionMatrix(classNames);
                                        
                                        confM.addPredictions((ArrayList<Prediction>) aux);
                                        
                                        if(Double.isNaN(confM.getTwoClassStats(indexPos).getRecall())){
                                            s += "0.0;";
                                        }else{
                                            s += confM.getTwoClassStats(indexPos).getRecall() + ";";
                                        }
                                        
                                        aux.clear();
                                        last += groups;
                                    }
                                }
                                
                                if(last < valuesPredict.size()){
                                    for(int g = last; g < valuesPredict.size(); g++){
                                        aux.add(valuesPredict.get(g));
                                    }
                                    
                                    confM = new ConfusionMatrix(classNames);
                                    
                                    confM.addPredictions((ArrayList<Prediction>) aux);
                                    
                                    if(Double.isNaN(confM.getTwoClassStats(indexPos).getRecall())){
                                        s += "0.0;";
                                    }else{
                                        s += confM.getTwoClassStats(indexPos).getRecall() + ";";
                                    }
                                    
                                    aux.clear();
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            int indexPos = foundInstances(objectResult.get().getInst());
                            List<Prediction> valuesPredict = evalResult.predictions();
                            List<Prediction> aux = new ArrayList<>();
                            int last = 0;
                            int cont = objectResult.get().getInst().numInstances()/groups;
                            int index = objectResult.get().getInst().classIndex();
                            String[] classNames = new String[objectResult.get().getInst().attribute(index).numValues()];

                            for(int value = 0; value < objectResult.get().getInst().attribute(index).numValues(); value++){
                                classNames[value] = objectResult.get().getInst().attribute(index).value(value);
                            }

                            for(int rest = 0; rest < cont; rest++){
                                if((last+groups) < valuesPredict.size()){
                                    for(int g = last; g < (last+groups); g++){
                                        aux.add(valuesPredict.get(g));
                                    }

                                    confM = new ConfusionMatrix(classNames);

                                    confM.addPredictions((ArrayList<Prediction>) aux);

                                    if(Double.isNaN(confM.getTwoClassStats(indexPos).getRecall())){
                                        s += "0.0;";
                                    }else{
                                        s += confM.getTwoClassStats(indexPos).getRecall() + ";";
                                    }

                                    aux.clear();
                                    last += groups;
                                }
                            }

                            if(last < valuesPredict.size()){
                                for(int g = last; g < valuesPredict.size(); g++){
                                    aux.add(valuesPredict.get(g));
                                }

                                confM = new ConfusionMatrix(classNames);

                                confM.addPredictions((ArrayList<Prediction>) aux);

                                if(Double.isNaN(confM.getTwoClassStats(indexPos).getRecall())){
                                    s += "0.0;";
                                }else{
                                    s += confM.getTwoClassStats(indexPos).getRecall() + ";";
                                }

                                aux.clear();
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List maeGroupsLOO(int groups) throws InterruptedException, ExecutionException, Exception{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                List<Prediction> valuesPredict = evalResult.predictions();
                                List<Prediction> aux = new ArrayList<>();
                                int last = 0;
                                int cont = objectResult.get().getInst().numInstances()/groups;

                                for(int rest = 0; rest < cont; rest++){
                                    if((last+groups) < valuesPredict.size()){
                                        for(int g = last; g < (last+groups); g++){
                                            aux.add(valuesPredict.get(g));
                                        }

                                        double sumatorio = 0.0;
                                        
                                        for(int a = 0; a < aux.size(); a++){
                                            sumatorio += Math.abs(aux.get(a).actual() - aux.get(a).predicted());
                                        }
                                        
                                        double mae = (1/groups) * sumatorio;
                                        
                                        s += mae + ";";
                                        aux.clear();
                                        last += groups;
                                    }
                                }
                                
                                if(last < valuesPredict.size()){
                                    for(int g = last; g < valuesPredict.size(); g++){
                                        aux.add(valuesPredict.get(g));
                                    }
                                    
                                    double sumatorio = 0.0;
                                        
                                    for(int a = 0; a < aux.size(); a++){
                                        sumatorio += Math.abs(aux.get(a).actual() - aux.get(a).predicted());
                                    }

                                    double mae = (1/groups) * sumatorio;

                                    s += mae + ";";
                                    
                                    aux.clear();
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            List<Prediction> valuesPredict = evalResult.predictions();
                            List<Prediction> aux = new ArrayList<>();
                            int last = 0;
                            int cont = objectResult.get().getInst().numInstances()/groups;

                            for(int rest = 0; rest < cont; rest++){
                                if((last+groups) < valuesPredict.size()){
                                    for(int g = last; g < (last+groups); g++){
                                        aux.add(valuesPredict.get(g));
                                    }

                                    double sumatorio = 0.0;

                                    for(int a = 0; a < aux.size(); a++){
                                        sumatorio += Math.abs(aux.get(a).actual() - aux.get(a).predicted());
                                    }

                                    double mae = (1/groups) * sumatorio;

                                    s += mae + ";";
                                    aux.clear();
                                    last += groups;
                                }
                            }

                            if(last < valuesPredict.size()){
                                for(int g = last; g < valuesPredict.size(); g++){
                                    aux.add(valuesPredict.get(g));
                                }

                                double sumatorio = 0.0;

                                for(int a = 0; a < aux.size(); a++){
                                    sumatorio += Math.abs(aux.get(a).actual() - aux.get(a).predicted());
                                }

                                double mae = (1/groups) * sumatorio;

                                s += mae + ";";

                                aux.clear();
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private List rmseGroupsLOO(int groups) throws InterruptedException, ExecutionException, Exception{
        List m = new ArrayList();
        String s = "";
        
        for(int i = 0; i < listEvaluators.size(); i++){
            String ev = getSpec(listEvaluators.get(i));
            String se = getSpec(listSearchAlgorithms.get(i));
            
            for(int j = 0; j < listClassifier.size(); j++){
                String c = getSpec(listClassifier.get(j));
                
                for(int z = 0; z < resultsAttrSelExp.size(); z++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);
                
                    if(objectResult.get().getEvaluator() != null && objectResult.get().getSearch() != null){
                        if(ev.equals(getSpec(objectResult.get().getEvaluator())) && se.equals(getSpec(objectResult.get().getSearch())) && c.equals(getSpec(objectResult.get().getClassifier()))){
                            Evaluation evalResult = objectResult.get().getEvalClassifier();

                            if(evalResult != null){
                                List<Prediction> valuesPredict = evalResult.predictions();
                                List<Prediction> aux = new ArrayList<>();
                                int last = 0;
                                int cont = objectResult.get().getInst().numInstances()/groups;

                                for(int rest = 0; rest < cont; rest++){
                                    if((last+groups) < valuesPredict.size()){
                                        for(int g = last; g < (last+groups); g++){
                                            aux.add(valuesPredict.get(g));
                                        }

                                        double sumatorio = 0.0;
                                        
                                        for(int a = 0; a < aux.size(); a++){
                                            sumatorio += Math.pow(Math.abs(aux.get(a).actual() - aux.get(a).predicted()), 2);
                                        }
                                        
                                        double rmse = Math.sqrt(sumatorio/groups);
                                        
                                        s += rmse + ";";
                                        aux.clear();
                                        last += groups;
                                    }
                                }
                                
                                if(last < valuesPredict.size()){
                                    for(int g = last; g < valuesPredict.size(); g++){
                                        aux.add(valuesPredict.get(g));
                                    }
                                    
                                    double sumatorio = 0.0;
                                        
                                    for(int a = 0; a < aux.size(); a++){
                                        sumatorio += Math.pow(Math.abs(aux.get(a).actual() - aux.get(a).predicted()), 2);
                                    }

                                    double rmse = Math.sqrt(sumatorio/groups);

                                    s += rmse + ";";
                                    
                                    aux.clear();
                                }
                            }
                        }
                    }
                }
                
                m.add(s);
                s = "";
            }
        }
        
        String ev = "original";
        String se = "original";
            
        for(int j = 0; j < listClassifier.size(); j++){
            String c = getSpec(listClassifier.get(j));

            for(int z = 0; z < resultsAttrSelExp.size(); z++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(z);

                if(objectResult.get().getEvaluator() == null && objectResult.get().getSearch() == null){
                    if(c.equals(getSpec(objectResult.get().getClassifier()))){
                        Evaluation evalResult = objectResult.get().getEvalClassifier();

                        if(evalResult != null){
                            List<Prediction> valuesPredict = evalResult.predictions();
                            List<Prediction> aux = new ArrayList<>();
                            int last = 0;
                            int cont = objectResult.get().getInst().numInstances()/groups;

                            for(int rest = 0; rest < cont; rest++){
                                if((last+groups) < valuesPredict.size()){
                                    for(int g = last; g < (last+groups); g++){
                                        aux.add(valuesPredict.get(g));
                                    }

                                    double sumatorio = 0.0;

                                    for(int a = 0; a < aux.size(); a++){
                                        sumatorio += Math.pow(Math.abs(aux.get(a).actual() - aux.get(a).predicted()), 2);
                                    }

                                    double rmse = Math.sqrt(sumatorio/groups);

                                    s += rmse + ";";
                                    aux.clear();
                                    last += groups;
                                }
                            }

                            if(last < valuesPredict.size()){
                                for(int g = last; g < valuesPredict.size(); g++){
                                    aux.add(valuesPredict.get(g));
                                }

                                double sumatorio = 0.0;

                                for(int a = 0; a < aux.size(); a++){
                                    sumatorio += Math.pow(Math.abs(aux.get(a).actual() - aux.get(a).predicted()), 2);
                                }

                                double rmse = Math.sqrt(sumatorio/groups);

                                s += rmse + ";";

                                aux.clear();
                            }
                        }
                    }
                }
            }

            m.add(s);
            s = "";
        }
        
        return m;
    }
    
    private void addInstancesToDatasetList (Instances dataset, int positiveClass) {
        dataset.setClassIndex(dataset.numAttributes() - 1);

        datasetsTableModel.addRow(new Object[]{dataset.relationName(), dataset.numInstances(), 
            dataset.numAttributes() - 1, dataset.numClasses(), dataset.attribute(dataset.numAttributes() - 1).name(), 
            dataset.attribute(dataset.classIndex()).value(positiveClass), listInstances.size()});

        listInstances.add(dataset);
        listClassPositive.add(positiveClass);

        checkRun();
    }
    
    public static String getSpec(Object object) {
        if (object instanceof OptionHandler) {
            return object.getClass().getSimpleName() + " "
                    + Utils.joinOptions(((OptionHandler)object).getOptions());
        }
        
        return object.getClass().getSimpleName();
    }
    
    private void checkRun() {
        if (!listInstances.isEmpty() && !listClassifier.isEmpty()) {
            runExpBtn.setEnabled(true);
        }else{
            runExpBtn.setEnabled(false);
        }
    }
    
    private boolean checkResult(){
        if(!resultsAttrSelExp.isEmpty()){
            return true;
        }
        
        return false;
    }
    
    private void checkLoadData(String r){
        if(r.equals("BBDD")){
            updatePredictionsBtn.setEnabled(false);
            datasetsListModel.removeAllElements();
            evaluatorListModel.removeAllElements();
            searchListModel.removeAllElements();
            classifierListModel.removeAllElements();
            attributesTextField.setText("");
            accuracyMetricsCheckBox.setSelected(true);
            precisionMetricsCheckBox.setSelected(true);
            recallMetricsCheckBox.setSelected(true);
            fMeasureMetricsCheckBox.setSelected(true);
            kappaMetricsCheckBox.setSelected(true);
            mccMetricsCheckBox.setSelected(true);
            aucMetricsCheckBox.setSelected(true);
            maeMetricsCheckBox.setSelected(true);
            mseMetricsCheckBox.setSelected(true);
            rmseMetricsCheckBox.setSelected(true);
            mapeMetricsCheckBox.setSelected(true);
            r2MetricsCheckBox.setSelected(true);
            accuracyMetricsCheckBox.setEnabled(false);
            precisionMetricsCheckBox.setEnabled(false);
            recallMetricsCheckBox.setEnabled(false);
            fMeasureMetricsCheckBox.setEnabled(false);
            kappaMetricsCheckBox.setEnabled(false);
            mccMetricsCheckBox.setEnabled(false);
            aucMetricsCheckBox.setEnabled(false);
            maeMetricsCheckBox.setEnabled(false);
            mseMetricsCheckBox.setEnabled(false);
            rmseMetricsCheckBox.setEnabled(false);
            mapeMetricsCheckBox.setEnabled(false);
            r2MetricsCheckBox.setEnabled(false);
            
            //Jpanel graph
            metricGraphComboBox.removeAllItems();
            xAxis1ComboBox.removeAllItems();
            xAxis2ComboBox.removeAllItems();
            barChartPanel.removeAll();
        }else{
            updatePredictionsBtn.setEnabled(true);
            accuracyMetricsCheckBox.setEnabled(true);
            precisionMetricsCheckBox.setEnabled(true);
            recallMetricsCheckBox.setEnabled(true);
            fMeasureMetricsCheckBox.setEnabled(true);
            kappaMetricsCheckBox.setEnabled(true);
            mccMetricsCheckBox.setEnabled(true);
            aucMetricsCheckBox.setEnabled(true);
            maeMetricsCheckBox.setEnabled(true);
            mseMetricsCheckBox.setEnabled(true);
            rmseMetricsCheckBox.setEnabled(true);
            mapeMetricsCheckBox.setEnabled(true);
            r2MetricsCheckBox.setEnabled(true);
            accuracyMetricsCheckBox.setSelected(false);
            precisionMetricsCheckBox.setSelected(false);
            recallMetricsCheckBox.setSelected(false);
            fMeasureMetricsCheckBox.setSelected(false);
            kappaMetricsCheckBox.setSelected(false);
            mccMetricsCheckBox.setSelected(false);
            aucMetricsCheckBox.setSelected(false);
            maeMetricsCheckBox.setSelected(false);
            mseMetricsCheckBox.setSelected(false);
            rmseMetricsCheckBox.setSelected(false);
            mapeMetricsCheckBox.setSelected(false);
            r2MetricsCheckBox.setSelected(false);
        }
    }

    private void completeTabResult() throws InterruptedException, ExecutionException{
        if(checkResult()){
            resetTabResult();

            int contEV = 1;
            
            for(int i = 0; i < listEvaluators.size(); i++){
                if(!captionEV.containsValue(getSpec(listEvaluators.get(i)))){
                    captionEV.put("E"+contEV, getSpec(listEvaluators.get(i)));
                    contEV++;
                }
                              
                captionSE.put("S"+(i+1), getSpec(listSearchAlgorithms.get(i)));
            }
            
            for(int i = 0; i < listClassifier.size(); i++){
                captionCL.put("C"+(i+1), getSpec(listClassifier.get(i)));
            }
            
            completeCaptionTable();
            completePredictionPanel();
            completeMetricsTable();
            completeGraphComboBox();
            completeStatisticalTestPanel();
        }
    }
    
    private void completeCaptionTable(){
        //caption evaluator and search
        for(int i = 0; i < captionEV.size(); i++){
            captionTableModel.addRow(new Object[]{"E" + (i+1), captionEV.get("E" + (i+1))});
        }
        
        for(int i = 0; i < captionSE.size(); i++){
            captionTableModel.addRow(new Object[]{"S" + (i+1), captionSE.get("S" + (i+1))});
        }
        
        //caption classifier
        for(int i = 0; i < captionCL.size(); i++){
            captionTableModel.addRow(new Object[]{"C" + (i+1), captionCL.get("C" + (i+1))});
        }
    }
    
    private void completePredictionPanel() throws InterruptedException, ExecutionException{
        //complete list of datasets
        for(int d = 0; d < listInstances.size(); d++){
            datasetsListModel.addElement(listInstances.get(d).relationName());
        }
        
        //complete lists of evaluators
        for(int e = 0; e < captionEV.size(); e++){
            evaluatorListModel.addElement("E"+(e+1));
        }
        
        //complete lists of searchs
        for(int i = 0; i < captionSE.size(); i++){
            searchListModel.addElement("S"+(i+1));
        }
        
        //no attribute selection
        evaluatorListModel.addElement("original");
        searchListModel.addElement("original");
        
        //complete list of classifiers
        for(int c = 0; c < captionCL.size(); c++){
            classifierListModel.addElement("C"+(c+1));
        }
        
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            String dS = objectResult.get().getInst().relationName();
            String cls = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));
            String eV = "original";
            String search = "original";
            
            if(objectResult.get().getEvaluator() != null){
                search = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                eV = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
            }
            
            Evaluation evalResult = objectResult.get().getEvalClassifier();
            
            if(evalResult != null){
                if(evalResult.predictions() != null){
                    List<Prediction> valuesPredict = evalResult.predictions();

                    for(int z = 0; z < valuesPredict.size(); z++){
                        predictionsTableModel.addRow(new Object[]{valuesPredict.get(z).actual(), valuesPredict.get(z).predicted(),
                        dS, eV, search, cls});
                    }
                }
            }
        }
        
        selectAllIndices(datasetsList, datasetsListModel.getSize());
        selectAllIndices(evaluatorList, evaluatorListModel.getSize());
        selectAllIndices(searchList, searchListModel.getSize());
        selectAllIndices(classifierList, classifierListModel.getSize());
    }
    
    private String findAlgorithms(Map<String, String> map, String s){
        int i = 0;
        boolean enc = false;
        
        Iterator it = map.values().iterator();
        
        while(it.hasNext() && !enc){
            String aux = (String)it.next();
            
            if(aux.equals(s)){
                enc = true;
            }else {
                i++;
            }
        }
        
        return (String) map.keySet().toArray()[i];
    }
    
    private void completeStatisticalTestPanel(){
        String[] dataComboBox;
        
        if(listInstances.get(0).classAttribute().isNominal()){
            if(leaveOneOutBtn.isSelected()){
                dataComboBox = new String[]{"Precision", "Recall", "F-measure"};
            }else{
                dataComboBox = new String[]{"Accuracy", "Precision", "F-measure", "Recall", "Kappa", "MCC", "AUC"};
            }
        }else{
            if(leaveOneOutBtn.isSelected()){
                dataComboBox = new String[]{"RMSE", "MAE"};
            }else{
                dataComboBox = new String[]{"MSE", "MAE", "RMSE", "MAPE", "R2"};
            }
        }

        metricStatisComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
        
        boolean dif = false;
        int j = 1;
        
        if(listInstances.get(0).classAttribute().isNominal()){
            while(j < listInstances.size() && !dif){
                if(!listInstances.get(j).classAttribute().isNominal()){
                    dif = true;
                }
                
                j++;
            }
            
            if(!dif){
                metricStatisComboBox.setSelectedIndex(2);
            }else{
                m_Log.logMessage("Some of the datasets are not nominal, so the statistical test cannot be done.");
            }
        }else{
            while(j < listInstances.size() && !dif){
                if(listInstances.get(j).classAttribute().isNominal()){
                    dif = true;
                }
                
                j++;
            }
            
            if(!dif){
                metricStatisComboBox.setSelectedIndex(1);
            }else{
                m_Log.logMessage("Some of the datasets are not numeric, so the statistical test cannot be done.");
            }
        }
        
        numDatasetsStatisValue.setText(Integer.toString(listInstances.size()));
        
        if(holdOutSplitBtn.isSelected()){
            numSamStatisValue.setText(Integer.toString(listInstances.size()));
        }else{
            if(crossValidationBtn.isSelected()){               
                numSamStatisValue.setText(Integer.toString(Integer.parseInt(crossValidationTextField.getText())*listInstances.size()));
            }else{
                int sum = 0;
                
                for(int i = 0; i < listInstances.size(); i++){
                    sum += listInstances.size();
                }
                
                numSamStatisValue.setText(Integer.toString(sum));
            }
        }
        
        valueGroupsLOOTextField.setText("30");
        
        dataComboBox = new String[]{"All", "Positives", "Negatives"};

        filterStatisComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
    }
    
    private void completeMetricsTable() throws InterruptedException, ExecutionException{
        Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(0);
        String e, s;
        
        if(objectResult.get().getEvaluator() != null){
            e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
            s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
        }else{
            e = "original";
            s = "original";
        }
        
        String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));
        
        if(objectResult.get().getEvaluator() != null){
            metricsTableModel.addRow(new Object[]{objectResult.get().getInst().relationName(), e, s, c, objectResult.get().getNumAttr()});
        }else{
            metricsTableModel.addRow(new Object[]{objectResult.get().getInst().relationName(), "original",
            "original", findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier())), objectResult.get().getNumAttr()});
        }
             
        for(int i = 1; i < resultsAttrSelExp.size(); i++){
            objectResult = resultsAttrSelExp.get(i);
            
            if(objectResult.get().getEvaluator() != null){
                e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
            }else{
                e = "original";
                s = "original";
            }
            
            c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));
            
            for(int j = 0; j < metricsTableModel.getRowCount(); j++){
                if(!isDifferent(e, s, c, objectResult.get().getInst().relationName())){
                    metricsTableModel.addRow(new Object[]{objectResult.get().getInst().relationName(), e, s, c, objectResult.get().getNumAttr()});   
                }
            }
        }
    }
    
    private boolean isDifferent(String ev, String se, String cls, String name){
        boolean enc = false;
        int e = 0;
        
        while(e < metricsTableModel.getRowCount() && !enc){
            if(ev.equals(metricsTableModel.getValueAt(e, 1)) && se.equals(metricsTableModel.getValueAt(e, 2)) && cls.equals(metricsTableModel.getValueAt(e, 3)) && name.equals(metricsTableModel.getValueAt(e, 0))){
                enc = true;
            }
            
            e++;
        }
        
        return enc;
    }
    
    private void completeGraphComboBox(){
        String[] dataComboBox = new String[]{"Accuracy", "Precision", "Recall", "F-measure", "Kappa", "MCC", "AUC", 
        "MAE", "MSE", "RMSE", "MAPE", "R2"};

        metricGraphComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
        
        dataComboBox = new String[]{"Dataset", "Evaluator", "Search", "Classifier"};

        xAxis1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
        xAxis2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
        
        //selected by default
        xAxis1ComboBox.setSelectedIndex(1);
        xAxis2ComboBox.setSelectedIndex(2);
        
        dataComboBox = new String[captionCL.size()];
        
        for(int i = 0; i < listClassifier.size(); i++){
            dataComboBox[i] = findAlgorithms(captionCL, getSpec(listClassifier.get(i)));
        }
        
        filterGraph1ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
        labelFilter1.setText("Classifier:");
        
        dataComboBox = new String[listInstances.size()];
        
        for(int i = 0; i < listInstances.size(); i++){
            dataComboBox[i] = listInstances.get(i).relationName();
        }
        
        filterGraph2ComboBox.setModel(new DefaultComboBoxModel(dataComboBox));
        labelFilter2.setText("Dataset:");
        
        if(listInstances.get(0).classAttribute().isNominal()){
            metricGraphComboBox.setSelectedIndex(0);
        }else{
            metricGraphComboBox.setSelectedIndex(7);
        }   
        
        updateGraphBtn.doClick();
    }
    
    private DefaultTableModel addColumnTableModel(String name, DefaultTableModel oldTableModel, List data){
        DefaultTableModel newTableModel = new DefaultTableModel();
        
        for(int cN = 0; cN < oldTableModel.getColumnCount(); cN++){
            newTableModel.addColumn(oldTableModel.getColumnName(cN));
        }
        
        for(int r = 0; r < oldTableModel.getRowCount(); r++) {
            Object[] fila = new Object[oldTableModel.getColumnCount()];

            for(int c = 0; c < fila.length; c++) {
                fila[c] = oldTableModel.getValueAt(r, c);
            }

            newTableModel.addRow(fila);
        }

        newTableModel.addColumn(name, data.toArray());
        
        return newTableModel;
    }
    
    private DefaultTableModel removeColumnTableModel(String name, DefaultTableModel oldTableModel){
        DefaultTableModel newTableModel = new DefaultTableModel();

        for(int cN = 0; cN < oldTableModel.getColumnCount(); cN++){
            if(!oldTableModel.getColumnName(cN).equals(name)){
                newTableModel.addColumn(oldTableModel.getColumnName(cN));
            }  
        }

        int s;

        for(int r = 0; r < oldTableModel.getRowCount(); r++) {
            Object[] fila = new Object[oldTableModel.getColumnCount()-1];
            s = 0;
            
            for(int c = 0; c < oldTableModel.getColumnCount(); c++) {
                if(!oldTableModel.getColumnName(c).equals(name)){
                    fila[c-s] = oldTableModel.getValueAt(r, c);
                }else{
                    s = 1;
                }
            }

            newTableModel.addRow(fila);
        }
        
        return newTableModel;
    }
    
    private void selectAllIndices(JList list, int size){
        int[] select = new int[size];
        
        for(int i = 0; i < size; i++){
            select[i] = i;
        }
        
        list.setSelectedIndices(select);
    }
    
    private int foundInstances(Instances instLost){
        boolean found = false;
        int j = 0;

        while(j < listInstances.size() && !found){
            if(listInstances.get(j).relationName().equals(instLost.relationName())){
                found = true;
            }
            j++;
        }
                
        return (j-1);
    }
    
    private List accuracy() throws InterruptedException, ExecutionException{
        List accuracy = new ArrayList(metricsTableModel.getRowCount());
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            try{
                                sum += evalResult.pctCorrect();
                            } catch(Exception ex){
                                sum += 0;
                                
                                m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                    objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                    objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                    objectResult.get().getClassifier().getClass().getSimpleName() +  
                                    " throw the exception: " + ex.getMessage());
                            }   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            accuracy.add(sum/same);
            same = 0;
            sum = 0.0;
        }
        
        return accuracy;
    }
    
    private List  precision() throws InterruptedException, ExecutionException{
        List precision = new ArrayList<>();       
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            try{
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.precision(listClassPositive.get(index)))){
                                    sum += 0.0;
                                }else{
                                    sum += evalResult.precision(listClassPositive.get(index));
                                }
                            } catch(InterruptedException | ExecutionException ex){
                                sum += 0;
                                
                                m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                    objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                    objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                    objectResult.get().getClassifier().getClass().getSimpleName() +  
                                    " throw the exception: " + ex.getMessage());
                            }   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            precision.add(sum/same);
            same = 0;
            sum = 0.0;
        }
        
        return precision;
    }
    
    private List recall() throws InterruptedException, ExecutionException{
        List recall = new ArrayList(metricsTableModel.getRowCount());               
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            try{
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.recall(listClassPositive.get(index)))){
                                    sum += 0.0;
                                }else{
                                    sum += evalResult.recall(listClassPositive.get(index));
                                }
                            } catch(InterruptedException | ExecutionException ex){
                                sum += 0;
                                
                                m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                    objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                    objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                    objectResult.get().getClassifier().getClass().getSimpleName() +  
                                    " throw the exception: " + ex.getMessage());
                            }   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            recall.add(sum/same);
            same = 0;
            sum = 0.0;
        }
        
        return recall;
    }
    
    private List fMeasure() throws InterruptedException, ExecutionException{
        List fMeasure = new ArrayList(metricsTableModel.getRowCount());                
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            try{
                                int index = foundInstances(objectResult.get().getInst());
                                
                                if(Double.isNaN(evalResult.fMeasure(listClassPositive.get(index)))){
                                    sum += 0.0;
                                }else{
                                    sum += evalResult.fMeasure(listClassPositive.get(index));
                                }
                            } catch(InterruptedException | ExecutionException ex){
                                sum += 0;
                                
                                m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                    objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                    objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                    objectResult.get().getClassifier().getClass().getSimpleName() +  
                                    " throw the exception: " + ex.getMessage());
                            }   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            fMeasure.add(sum/same);
            same = 0;
            sum = 0.0;
        }
        
        return fMeasure;
    }
    
    private List kappa() throws InterruptedException, ExecutionException{
        List kappa = new ArrayList(metricsTableModel.getRowCount());
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            sum += evalResult.kappa();   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            kappa.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return kappa;
    }
    
    private List mcc() throws InterruptedException, ExecutionException{
        List mcc = new ArrayList(metricsTableModel.getRowCount());       
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            int index = foundInstances(objectResult.get().getInst());
                            
                            if(Double.isNaN(evalResult.matthewsCorrelationCoefficient(listClassPositive.get(index)))){
                                sum += 0.0;
                            }else{
                                sum += evalResult.matthewsCorrelationCoefficient(listClassPositive.get(index));  
                            }
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            mcc.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return mcc;
    }
    
    private List auc() throws InterruptedException, ExecutionException{
        List auc = new ArrayList(metricsTableModel.getRowCount());
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            int index = foundInstances(objectResult.get().getInst());
                            
                            if(Double.isNaN(evalResult.areaUnderROC(listClassPositive.get(index)))){
                                sum += 0.0;
                            }else{
                                sum += evalResult.areaUnderROC(listClassPositive.get(index));
                            }
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            auc.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return auc; 
    }
    
    private List mae() throws InterruptedException, ExecutionException{
        List mae = new ArrayList(metricsTableModel.getRowCount());
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && !objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            sum += evalResult.meanAbsoluteError();   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            mae.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return mae; 
    }
    
    private List mse() throws InterruptedException, ExecutionException{
        List mse = new ArrayList(metricsTableModel.getRowCount());  
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && !objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            sum += Math.pow(evalResult.rootMeanSquaredError(), 2);   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            mse.add(sum/same);
            same = 0;
            sum = 0.0;
        }
        
        return mse;
    }
    
    private List rmse() throws InterruptedException, ExecutionException{
        List rmse = new ArrayList(metricsTableModel.getRowCount());
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && !objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            sum += evalResult.rootMeanSquaredError();   
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            rmse.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return rmse;
    }
    
    private List mape() throws InterruptedException, ExecutionException{
        List mape = new ArrayList(metricsTableModel.getRowCount());
        int size = 0;
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && !objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            try{
                                double aux = 0.0;

                                List<Prediction> valuesPredict = evalResult.predictions();

                                for(int z = 0; z < valuesPredict.size(); z++){
                                    aux += Math.abs(valuesPredict.get(z).actual() - valuesPredict.get(z).predicted())/valuesPredict.get(z).predicted();
                                    size++;
                                }

                                sum += (aux/size)*100;
                                size = 0;
                            } catch(Exception ex){
                                sum += 0;
                                m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                    objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                    objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                    objectResult.get().getClassifier().getClass().getSimpleName() +  
                                    " throw the exception: " + ex.getMessage());
                            }
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            mape.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return mape;
    }
    
    private List r2() throws InterruptedException, ExecutionException{
        List r2 = new ArrayList(metricsTableModel.getRowCount());
        double sum = 0.0;        
        int same = 0;
        
        for(int j = 0; j < metricsTableModel.getRowCount(); j++){
            for(int i = 0; i < resultsAttrSelExp.size(); i++){
                Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                String e, s;

                if(objectResult.get().getEvaluator() != null){
                    e = findAlgorithms(captionEV, getSpec(objectResult.get().getEvaluator()));
                    s = findAlgorithms(captionSE, getSpec(objectResult.get().getSearch()));
                }else{
                    e = "original";
                    s = "original";
                }

                String c = findAlgorithms(captionCL, getSpec(objectResult.get().getClassifier()));

                if(e.equals(metricsTableModel.getValueAt(j, 1)) && s.equals(metricsTableModel.getValueAt(j, 2)) && c.equals(metricsTableModel.getValueAt(j, 3)) && objectResult.get().getInst().relationName().equals(metricsTableModel.getValueAt(j, 0))){
                    Evaluation evalResult = objectResult.get().getEvalClassifier();

                    if(evalResult != null && !objectResult.get().getInst().classAttribute().isNominal()){
                        if(evalResult.predictions() != null){
                            try {   
                                sum += evalResult.correlationCoefficient();
                            } catch (Exception ex) {
                                m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                    objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                    objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                    objectResult.get().getClassifier().getClass().getSimpleName() +  
                                    " throw the exception: " + ex.getMessage());
                            }
                        }else{
                            sum += 0;
                        }  
                    }else{
                        sum += 0;
                    }
                    
                    same++;
                }
            }
            
            r2.add(sum/same);
            same = 0;
            sum = 0.0;
        }
                
        return r2;
    }
    
    private Instances createObjectInstances(DefaultTableModel tableModel, String name){
        ArrayList<Attribute> listAttributes = new ArrayList<>();
        int nominalColumns = 0;
        
        for(int i = 0; i < tableModel.getColumnCount(); i++){
            Attribute a = null;

            if(tableModel.getColumnName(i).equals("Actual") || tableModel.getColumnName(i).equals("Predicted") || tableModel.getColumnName(i).equals("Accuracy") || tableModel.getColumnName(i).equals("NumAttributes") || 
                tableModel.getColumnName(i).equals("Precision") || tableModel.getColumnName(i).equals("Recall") || tableModel.getColumnName(i).equals("F-measure") || tableModel.getColumnName(i).equals("Kappa") ||
                tableModel.getColumnName(i).equals("MCC") || tableModel.getColumnName(i).equals("AUC") || tableModel.getColumnName(i).equals("MAE") || tableModel.getColumnName(i).equals("MSE") || tableModel.getColumnName(i).equals("RMSE") ||
                tableModel.getColumnName(i).equals("MAPE") || tableModel.getColumnName(i).equals("R2") || tableModel.getColumnName(i).equals("p(left)") || tableModel.getColumnName(i).equals("p(right)")
                || tableModel.getColumnName(i).equals("p(no)")){
                a = new Attribute(tableModel.getColumnName(i));
            }else{
                a = new Attribute(tableModel.getColumnName(i), (ArrayList<String>) null);
                nominalColumns++;
            }

            listAttributes.add(a);
        }

        Instances tableInst = new Instances(name, listAttributes, 0);

        double[] values = null;
        
        for(int r = 0; r < tableModel.getRowCount(); r++){
            values = new double[tableInst.numAttributes()];
            
            for(int c = 0; c < tableModel.getColumnCount(); c++){
                if(tableModel.getValueAt(r, c) instanceof Double){
                    values[c] = (double) tableModel.getValueAt(r, c);
                }else if(tableModel.getValueAt(r, c) instanceof Integer){
                    values[c] = (int) tableModel.getValueAt(r, c);
                }else if(tableModel.getValueAt(r, c) instanceof String){                   
                    if(tableModel.getColumnName(c).equals("Evaluator") && !(((String)tableModel.getValueAt(r, c)).equals("original"))){
                        values[c] = tableInst.attribute(c).addStringValue(captionEV.get((String)tableModel.getValueAt(r, c)));
                    }else if(tableModel.getColumnName(c).equals("Search") && !(((String)tableModel.getValueAt(r, c)).equals("original"))){
                        values[c] = tableInst.attribute(c).addStringValue(captionSE.get((String)tableModel.getValueAt(r, c)));
                    }else if(tableModel.getColumnName(c).equals("Classifier") && !(((String)tableModel.getValueAt(r, c)).equals("original"))){
                        values[c] = tableInst.attribute(c).addStringValue(captionCL.get((String)tableModel.getValueAt(r, c)));
                    }else{
                        values[c] = tableInst.attribute(c).addStringValue((String)tableModel.getValueAt(r, c));
                    }
                }
            }
            
            Instance row = new DenseInstance(1.0, values);
            tableInst.add(row);
        }
        
        try {
            StringToNominal f = new StringToNominal();
            
            f.setInputFormat(tableInst);
            
            String[] o = new String[]{"-R", ""};
            
            for(int r = 0; r < nominalColumns; r++){
                if(r == 0){
                    o[1] = o[1] + (r+1);
                }else{
                    o[1] = o[1] + "," +(r+1);
                }
            }
            
            f.setOptions(o);
            tableInst = Filter.useFilter(tableInst, f);
        } catch (Exception ex) {
            m_Log.statusMessage("Error");
        }
        
        return tableInst;
    }
    
    private void saveCSVTable(Instances saveInst){
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser fileChooser = new JFileChooser(new File(initialDir));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV file: comma separated file (*.csv)", "csv");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showSaveDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            CSVSaver saver = new CSVSaver();
            saver.setInstances(saveInst);
            try {
                File out = new File(fileChooser.getSelectedFile() + ".csv");
                saver.setFile(out);
                saver.writeBatch();
            } catch (IOException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void saveArffTable(Instances saveInst) throws Exception{
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser fileChooser = new JFileChooser(new File(initialDir));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arff data files (*.arff)", "arff");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showSaveDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            DataSink.write(fileChooser.getSelectedFile() + ".arff", saveInst);
        }
    }
    
    private void saveLatexTable(Instances saveInst, String tableName){
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser fileChooser = new JFileChooser(new File(initialDir));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Latex", "tex");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showSaveDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String contentFile = instancesToLatex(saveInst, tableName);
            File conf = new File(fileChooser.getSelectedFile() + ".tex");
            
            BufferedWriter bw;
            
            if(!conf.exists()){
                try {
                    bw = new BufferedWriter(new FileWriter(conf));
                    bw.write(contentFile, 0, contentFile.length());
                    bw.close();
                } catch (IOException ex) {
                    m_Log.logMessage("Error creating configuration file");
                    m_Log.statusMessage("See error log");
                }
            }
        }
    }
    
    public static String instancesToLatex(Instances inst, String tableName){
        int next = 0;
        String latexContent = "\\begin{table}[ht]\n" +
                            "\\begin{center}\n" +
                            "\\setlength{\\tabcolsep}{10pt}\n" +
                            "\\renewcommand{\\arraystretch}{1.5}\n" +
                            "\\caption{Table caption.}\n" +
                            "\\begin{tabular}{";
        
        if(tableName.equals("Metrics")){
            latexContent += "|l|l|l|l|l|";
            next = 5;
        }
        
        for(int attr = next; attr < inst.numAttributes()-1; attr++){
            latexContent += "c|";
        }
        
        latexContent += "c|}\n" + "\\hline\n";
        
        for(int attr = 0; attr < inst.numAttributes()-1; attr++){
            latexContent += inst.attribute(attr).name() + " & ";
        }
        
        latexContent += inst.attribute(inst.numAttributes()-1).name() + "\\\\ \\hline\n";
        
        DecimalFormat df = new DecimalFormat("###.###");
        
        for(int indexInst = 0; indexInst < inst.numInstances()-1; indexInst++){
            for(int attr = 0; attr < inst.numAttributes()-1; attr++){
                if(inst.attribute(attr).isNumeric()){
                    double b = inst.get(indexInst).value(attr);
                    
                    if(Double.isNaN(b)){
                        latexContent += " & ";
                    } else{
                        latexContent += String.valueOf(df.format(b)) + " & ";
                    }
                } else {
                    latexContent += inst.get(indexInst).attribute(attr).value((int)inst.get(indexInst).value(attr)) + " & ";
                }
            }
            
            //For the value of the last attribute
            if(inst.attribute(inst.numAttributes()-1).isNumeric()){
                double b = inst.get(indexInst).value(inst.numAttributes()-1);
                
                if(Double.isNaN(b)){
                    latexContent += "\\\\\n";
                } else {
                    latexContent += String.valueOf(df.format(b)) + "\\\\\n";
                }
            } else{
                latexContent += inst.get(indexInst).attribute(inst.numAttributes()-1).value((int)inst.get(indexInst).value(inst.numAttributes()-1)) + "\\\\\n";
            }
        }
        
        //For the value of the last instance
        int lastInst = inst.numInstances() - 1;
        
        for(int attr = 0; attr < inst.numAttributes()-1; attr++){
            if(inst.attribute(attr).isNumeric()){
                double b = inst.get(lastInst).value(attr);
                
                if(Double.isNaN(b)){
                    latexContent += " & ";
                } else {
                    latexContent += String.valueOf(df.format(b)) + " & ";
                }
            } else {
                latexContent += inst.get(lastInst).attribute(attr).value((int)inst.get(lastInst).value(attr)) + " & ";
            }
        }
        
        if(inst.attribute(inst.numAttributes()-1).isNumeric()){
            double b = inst.get(lastInst).value(inst.numAttributes()-1);
            
            if(Double.isNaN(b)){
                latexContent += "\\\\ \\hline\n";
            } else {
                latexContent += String.valueOf(df.format(b)) + "\\\\ \\hline\n";
            }
        }else{
            latexContent += inst.get(lastInst).attribute(inst.numAttributes()-1).value((int)inst.get(lastInst).value(inst.numAttributes()-1)) + "\\\\ \\hline\n";
        }
        
        latexContent += "\\end{tabular}\n" +
                        "\\label{tab:table}\n" +
                        "\\end{center}\n" +
                        "\\end{table}";
        
        return latexContent;
    }
    
    private void savePNGGraph(){
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser fileChooser = new JFileChooser(new File(initialDir));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG", "png");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showSaveDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = new File(fileChooser.getSelectedFile() + ".png");
            try {
                //ChartUtilities.saveChartAsPNG(f, chart, 600, 400);
                ChartUtilities.saveChartAsPNG(f, chart, 1280, 720);
            } catch (IOException ex) {
                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void savePDFGraph(){
        String initialDir = ExplorerDefaults.getInitialDirectory();
        JFileChooser fileChooser = new JFileChooser(new File(initialDir));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PDF", "pdf");
        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showSaveDialog(this);
        
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File f = new File(fileChooser.getSelectedFile() + ".pdf");
            PDFDocument pdfDoc = new PDFDocument();
            
            Page page = pdfDoc.createPage(new Rectangle(1280, 720));
            PDFGraphics2D g2 = page.getGraphics2D();
            
            chart.draw(g2, new Rectangle(0, 0, 1280, 720));
            pdfDoc.writeToFile(f);
        }
    }
    
    private void existBBDD(){
        Properties p = new Properties();
        String dir = System.getProperty("user.home");
        String fileSeparator = File.separator;
        
        try {
            if(fileSeparator.equals("\\")){
                p.load(new FileReader( dir + "\\wekafiles\\packages\\FS-Studio\\DatabaseUtils.props"));
            }else{
                p.load(new FileReader( dir + "/wekafiles/packages/FS-Studio/DatabaseUtils.props"));
            }
        } catch (IOException ex) {
            m_Log.logMessage("The properties file cannot be read");
            m_Log.statusMessage("See error log");
        }

        String driver = p.getProperty("jdbcDriver");
        
        try {
           Class.forName(driver);
        } catch (ClassNotFoundException ex){
           m_Log.logMessage("Driver not found");
           m_Log.statusMessage("See error log");
        }
        
        Connection conn = null;
        String sURL = p.getProperty("jdbcURL");
        String user = p.getProperty("user");
        String password = p.getProperty("password");
        
        try {
            conn = DriverManager.getConnection(sURL, user, password);
            
            try (Statement st = conn.createStatement()){
                //check if we can access the database
                ResultSet rs = st.executeQuery("SELECT * FROM attrselexp.experiment_group");
            } catch (SQLException sqle) { 
                ScriptRunner runner = new ScriptRunner(conn, false, false);
                String file;
                
                if(fileSeparator.equals("\\")){
                    file =  dir + "\\wekafiles\\packages\\FS-Studio\\DB\\DB.sql";
                }else{
                    file =  dir + "/wekafiles/packages/FS-Studio/DB/DB.sql";
                }
                
                try {
                    runner.runScript(new BufferedReader(new FileReader(file)));
                } catch (IOException ex) {
                    m_Log.logMessage("No connection to the database");
                }
            }
        } catch (SQLException ex) {
            m_Log.logMessage("No connection to the database");
            m_Log.statusMessage("See error log");
        }
    }
    
    private void saveBBDD(){
        if(resultsAttrSelExp != null){
            existBBDD();
            Properties p = new Properties();
            String dir = System.getProperty("user.home");
            String fileSeparator = File.separator;

            try {
                if(fileSeparator.equals("\\")){
                    p.load(new FileReader( dir + "\\wekafiles\\packages\\FS-Studio\\DatabaseUtils.props"));
                }else{
                    p.load(new FileReader( dir + "/wekafiles/packages/FS-Studio/DatabaseUtils.props"));
                }
            } catch (IOException ex) {
                m_Log.logMessage("The properties file cannot be read");
                m_Log.statusMessage("See error log");
            }

            String driver = p.getProperty("jdbcDriver");

            try {
               Class.forName(driver);
            } catch (ClassNotFoundException ex){
               m_Log.logMessage("Driver not found");
               m_Log.statusMessage("See error log");
            }

            Connection conn = null;
            String sURL = p.getProperty("jdbcURL") + "attrselexp";
            String user = p.getProperty("user");
            String password = p.getProperty("password");

            try {
                conn = DriverManager.getConnection(sURL, user, password);

                try (Statement st = conn.createStatement()){
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

                    try{
                        st.executeUpdate("INSERT INTO experiment_group (datetime) VALUES ('"+dtf.format(LocalDateTime.now())+"')");
                    } catch(Exception ex){
                        ScriptRunner runner = new ScriptRunner(conn, false, false);
                        String file;
                        
                        if(fileSeparator.equals("\\")){
                            file =  dir + "\\wekafiles\\packages\\FS-Studio\\DB\\experiment_group.sql";
                        }else{
                            file =  dir + "/wekafiles/packages/FS-Studio/DB/experiment_group.sql";
                        }
                        
                        try {
                            runner.runScript(new BufferedReader(new FileReader(file)));
                            
                            st.executeUpdate("INSERT INTO experiment_group (datetime) VALUES ('"+dtf.format(LocalDateTime.now())+"')");
                        } catch (IOException IOex) {
                            Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, IOex);
                        }
                    }

                    ResultSet rs = st.executeQuery("SELECT MAX(id) FROM experiment_group");
                    int id = 0;

                    if(rs.next()){
                        id = rs.getInt(1);
                    }

                    List accuracy = null;
                    List precision = null;
                    List recall = null;
                    List fMeasure = null;
                    List kappa = null;
                    List mcc = null;
                    List auc = null;
                    List mae = null;
                    List mse = null;
                    List rmse = null;
                    List mape = null;
                    List r2 = null;

                    try {
                        accuracy = accuracy();
                        precision = precision();
                        recall = recall();
                        fMeasure = fMeasure();
                        kappa = kappa();
                        mcc = mcc();
                        auc = auc();
                        mae = mae();
                        mse = mse();
                        rmse = rmse();
                        mape = mape();
                        r2 = r2();
                    } catch (InterruptedException | ExecutionException ex) {
                        m_Log.logMessage("Could not calculate metrics");
                        m_Log.statusMessage("See error log");
                    }

                    List<String> datasetsSelect = datasetsList.getSelectedValuesList();
                    List<String> evalsSelect = evaluatorList.getSelectedValuesList();
                    List<String> searchsSelect = searchList.getSelectedValuesList();
                    List<String> clsSelect = classifierList.getSelectedValuesList();

                    for (int i = 0; i < resultsAttrSelExp.size(); i++){
                        Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
                        String dt = (String)metricsTableModel.getValueAt(i, 0);
                        String eval = captionEV.get((String)metricsTableModel.getValueAt(i, 1));
                        String search = captionSE.get((String)metricsTableModel.getValueAt(i, 2));
                        String cls = captionCL.get((String)metricsTableModel.getValueAt(i, 3));
                        int numAttr = (int)metricsTableModel.getValueAt(i, 4);

                        try{
                            st.executeUpdate("INSERT INTO experiment (idexperiment_group, dataset, evaluator, search, classifier) "
                            + "VALUES ('"+id+"','"+dt+"','"+eval+"','"+search+"','"+cls+"')");
                        } catch(Exception ex){
                            ScriptRunner runner = new ScriptRunner(conn, false, false);
                            String file;
                            
                            if(fileSeparator.equals("\\")){
                                file =  dir + "\\wekafiles\\packages\\FS-Studio\\DB\\experiment.sql";
                            }else{
                                file =  dir + "/wekafiles/packages/FS-Studio/DB/experiment.sql";
                            }
                            
                            try {
                                runner.runScript(new BufferedReader(new FileReader(file)));
                                
                                st.executeUpdate("INSERT INTO experiment (idexperiment_group, dataset, evaluator, search, classifier) "
                                + "VALUES ('"+id+"','"+dt+"','"+eval+"','"+search+"','"+cls+"')");
                            } catch (IOException IOex) {
                                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, IOex);
                            }
                        }

                        ResultSet rs2 = st.executeQuery("SELECT MAX(id) FROM experiment");
                        int idExp = 0;

                        if(rs2.next()){
                            idExp = rs2.getInt(1);
                        }

                        //save metrics
                        try{
                            st.executeUpdate("INSERT INTO metrics (idexperiment, dataset, evaluator, search, classifier, num_attr, accuracy, precision_value, recall, f_measure,"
                                    + "kappa, mcc, auc, mae, mse, rmse, mape, r2) VALUES ('"+idExp+"','"+dt+"','"+eval+"','"+search+"','"+cls+"','"+numAttr+"','"+accuracy.get(i)+"','"+precision.get(i)+"'"
                                    + ",'"+recall.get(i)+"','"+fMeasure.get(i)+"','"+kappa.get(i)+"','"+mcc.get(i)+"','"+auc.get(i)+"','"+mae.get(i)+"','"+mse.get(i)+"','"+rmse.get(i)+
                                    "','"+mape.get(i)+"','"+r2.get(i)+"')");
                        } catch(Exception ex){
                            ScriptRunner runner = new ScriptRunner(conn, false, false);
                            String file;
                            
                            if(fileSeparator.equals("\\")){
                                file =  dir + "\\wekafiles\\packages\\FS-Studio\\DB\\metrics.sql";
                            }else{
                                file =  dir + "/wekafiles/packages/FS-Studio/DB/metrics.sql";
                            }
                            
                            try {
                                runner.runScript(new BufferedReader(new FileReader(file)));
                                
                                st.executeUpdate("INSERT INTO metrics (idexperiment, dataset, evaluator, search, classifier, num_attr, accuracy, precision_value, recall, f_measure,"
                                    + "kappa, mcc, auc, mae, mse, rmse, mape, r2) VALUES ('"+idExp+"','"+dt+"','"+eval+"','"+search+"','"+cls+"','"+numAttr+"','"+accuracy.get(i)+"','"+precision.get(i)+"'"
                                    + ",'"+recall.get(i)+"','"+fMeasure.get(i)+"','"+kappa.get(i)+"','"+mcc.get(i)+"','"+auc.get(i)+"','"+mae.get(i)+"','"+mse.get(i)+"','"+rmse.get(i)+
                                    "','"+mape.get(i)+"','"+r2.get(i)+"')");
                            } catch (IOException IOex) {
                                Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, IOex);
                            }
                        }

                        //example of a element of data = "27;78;175"
                        List<String> data = new ArrayList();
                        List<String> dataIndexAttrSel = new ArrayList();

                        if(!attributesTextField.getText().trim().equals("")){
                            String[] attrSelected = attributesTextField.getText().split(",");
                            int indexAttrSel;

                            try {
                                Instances test = objectResult.get().getTest();

                                for(int j = 0; j < 1; j++){
                                    try{
                                        indexAttrSel = Integer.parseInt(attrSelected[j].trim())-1;

                                        for(int t = 0; t < test.size(); t++){
                                            if(test.get(t).attribute(indexAttrSel).isNumeric()){
                                                data.add(String.valueOf(test.get(t).value(indexAttrSel)));
                                            } else {
                                                data.add(test.get(t).attribute(indexAttrSel).value((int)test.get(t).value(indexAttrSel)));
                                            }

                                            dataIndexAttrSel.add(String.valueOf(indexAttrSel));
                                        }         
                                        m_Log.statusMessage("OK");
                                    } catch(Exception ex){
                                        m_Log.logMessage("Exception: " + ex.getMessage());
                                        m_Log.statusMessage("See error log");
                                    }  
                                }

                                for(int j = 1; j < attrSelected.length; j++){
                                    try{
                                        indexAttrSel = Integer.parseInt(attrSelected[j].trim())-1;

                                        for(int t = 0; t < test.size(); t++){
                                            String aux = data.get(t);
                                            String indexAux = dataIndexAttrSel.get(t);

                                            if(test.get(t).attribute(indexAttrSel).isNumeric()){
                                                aux += ";" + String.valueOf(test.get(t).value(indexAttrSel));
                                                data.set(t, aux);
                                            } else {
                                                aux += ";" + test.get(t).attribute(indexAttrSel).value((int)test.get(t).value(indexAttrSel));
                                                data.set(t, aux);
                                            }

                                            indexAux += ";" + String.valueOf(indexAttrSel);
                                            dataIndexAttrSel.set(t, indexAux);
                                        }         
                                        m_Log.statusMessage("OK");
                                    } catch(Exception ex){
                                        m_Log.logMessage("Exception: " + ex.getMessage());
                                        m_Log.statusMessage("See error log");
                                    }  
                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                m_Log.logMessage("Exception: " + ex.getMessage());
                                m_Log.statusMessage("See error log");
                            }
                        }

                        try {
                            List<Prediction> valuesPredict = objectResult.get().getEvalClassifier().predictions();

                            if(!data.isEmpty()){
                                for(int z = 0; z < data.size(); z++){
                                    try{
                                        st.executeUpdate("INSERT INTO predictions (idexperiment, actual, predicted, dataset, evaluator, search, classifier, attributes, index_attr_selected) "
                                        + "VALUES ('"+idExp+"','"+valuesPredict.get(z).actual()+"','"+valuesPredict.get(z).predicted()+"','"+dt+"','"+eval+"','"+search+"','"+cls+"','"+data.get(z)+"','"+dataIndexAttrSel.get(z)+"')");
                                    } catch(Exception ex){
                                        ScriptRunner runner = new ScriptRunner(conn, false, false);
                                        String file;
                                        
                                        if(fileSeparator.equals("\\")){
                                            file =  dir + "\\wekafiles\\packages\\FS-Studio\\DB\\predictions.sql";
                                        }else{
                                            file =  dir + "/wekafiles/packages/FS-Studio/DB/predictions.sql";
                                        }
                                        
                                        try {
                                            runner.runScript(new BufferedReader(new FileReader(file)));

                                            st.executeUpdate("INSERT INTO predictions (idexperiment, actual, predicted, dataset, evaluator, search, classifier, attributes, index_attr_selected) "
                                            + "VALUES ('"+idExp+"','"+valuesPredict.get(z).actual()+"','"+valuesPredict.get(z).predicted()+"','"+dt+"','"+eval+"','"+search+"','"+cls+"','"+data.get(z)+"','"+dataIndexAttrSel.get(z)+"')");
                                        } catch (IOException IOex) {
                                            Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, IOex);
                                        }
                                    }
                                }

                                data.clear();
                            }else{
                                for(int z = 0; z < valuesPredict.size(); z++){
                                    try{
                                        st.executeUpdate("INSERT INTO predictions (idexperiment, actual, predicted, dataset, evaluator, search, classifier) "
                                        + "VALUES ('"+idExp+"','"+valuesPredict.get(z).actual()+"','"+valuesPredict.get(z).predicted()+"','"+dt+"','"+eval+"','"+search+"','"+cls+"')");
                                    } catch(Exception ex){
                                        ScriptRunner runner = new ScriptRunner(conn, false, false);
                                        String file;
                                        
                                        if(fileSeparator.equals("\\")){
                                            file =  dir + "\\wekafiles\\packages\\FS-Studio\\DB\\predictions.sql";
                                        }else{
                                            file =  dir + "/wekafiles/packages/FS-Studio/DB/predictions.sql";
                                        }
                                        
                                        try {
                                            runner.runScript(new BufferedReader(new FileReader(file)));

                                            st.executeUpdate("INSERT INTO predictions (idexperiment, actual, predicted, dataset, evaluator, search, classifier) "
                                            + "VALUES ('"+idExp+"','"+valuesPredict.get(z).actual()+"','"+valuesPredict.get(z).predicted()+"','"+dt+"','"+eval+"','"+search+"','"+cls+"')");
                                        } catch (IOException IOex) {
                                            Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, IOex);
                                        }
                                    }
                                    
                                }
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            m_Log.logMessage(ex.getMessage());
                        }  
                    }

                    st.close();
                    conn.close();
                } catch (SQLException sqle) { 
                    m_Log.logMessage("Error in the execution:" + sqle.getErrorCode() + " " + sqle.getMessage());  
                    m_Log.statusMessage("See error log");
                }
            } catch (SQLException ex) {
                m_Log.logMessage(ex.getMessage());
                m_Log.statusMessage("See error log");
            }
        }else{
            m_Log.logMessage("No experiment has been run");
            m_Log.statusMessage("See error log");
        }    
    }
    
    private void loadBBDD(){
        Properties p = new Properties();
        String dir = System.getProperty("user.home");
        String fileSeparator = File.separator;

        try {
            if(fileSeparator.equals("\\")){
                p.load(new FileReader( dir + "\\wekafiles\\packages\\FS-Studio\\DatabaseUtils.props"));
            }else{
                p.load(new FileReader( dir + "/wekafiles/packages/FS-Studio/DatabaseUtils.props"));
            }
        }  catch (IOException ex) {
            m_Log.logMessage("The properties file cannot be read");
            m_Log.statusMessage("See error log");
        }
        
        String driver = p.getProperty("jdbcDriver");
        
        try {
           Class.forName(driver);
        } catch (ClassNotFoundException ex){
           m_Log.logMessage("Driver not found");
           m_Log.statusMessage("See error log");
        }
        
        Connection conn = null;
        String sURL = p.getProperty("jdbcURL") + "attrselexp";
        String user = p.getProperty("user");
        String password = p.getProperty("password");
        
        try {
            conn = DriverManager.getConnection(sURL, user, password);
            
            try (Statement st = conn.createStatement()){
                ResultSet rs = st.executeQuery("SELECT * FROM experiment_group");
                List experimentGroups = new ArrayList();
                
                while(rs.next()){
                    experimentGroups.add("Id: " + rs.getInt(1) + ", Datetime: " + rs.getString(2));
                }
                
                Object selection = JOptionPane.showInputDialog(null,"Choose a experiment group", "Selection experiment", 
                        JOptionPane.QUESTION_MESSAGE, null, experimentGroups.toArray(), null);

                if(selection != null){
                    String expSel = (String)selection;
                    int idExpSel = Integer.parseInt(expSel.split(",")[0].split(":")[1].trim());
                    boolean columnAttrCreate = false;
                    DefaultTableModel newTableModel = new DefaultTableModel();

                    for(int cN = 0; cN < 6; cN++){
                        newTableModel.addColumn(predictionsTableModel.getColumnName(cN));
                    }
                    
                    ResultSet rs2 = st.executeQuery("SELECT id FROM experiment WHERE idexperiment_group = " + idExpSel);
                    
                    List ids = new ArrayList();
                    
                    while(rs2.next()){
                        ids.add(rs2.getString(1));
                    }
                    
                    //load table predictions
                    for(int id = 0; id < ids.size(); id++){
                        ResultSet rs3 = st.executeQuery("SELECT actual, predicted, dataset, evaluator, search,"
                                + "classifier, attributes, index_attr_selected FROM predictions WHERE idexperiment = " + ids.get(id));                        
                        
                        while(rs3.next()){
                            if(!columnAttrCreate && (rs3.getString("index_attr_selected")!= null)){
                                String[] indexAttr = rs3.getString("index_attr_selected").split(";");
                                
                                for(int i = 0; i < indexAttr.length; i++){
                                    newTableModel.addColumn("Attribute " + (Integer.parseInt(indexAttr[i])+1));
                                }
                                
                                columnAttrCreate = true;
                            }
                            
                            Object[] fila = new Object[newTableModel.getColumnCount()];
                            int j;
                            
                            fila[0] = rs3.getDouble("actual");
                            fila[1] = rs3.getDouble("predicted");
                            
                            for(j = 3; j <= 6; j++) {
                                fila[j-1] = rs3.getString(j);
                            }

                            if(rs3.getString("attributes") != null){
                                String[] attr = rs3.getString("attributes").split(";");
                                int z = 0;

                                for(; j <= fila.length; j++){
                                    fila[j-1] = attr[z];
                                    z++;
                                }
                            }
                            
                            newTableModel.addRow(fila);
                        }
                    }
                    
                    predictionsTableModel = newTableModel;
                    predictionsTable.setModel(predictionsTableModel);
                    
                    DefaultTableModel newTableModelMetrics = new DefaultTableModel();

                    for(int cN = 0; cN < 5; cN++){
                        newTableModelMetrics.addColumn(metricsTableModel.getColumnName(cN));
                    }
                    
                    newTableModelMetrics.addColumn("Accuracy");
                    newTableModelMetrics.addColumn("Precision");
                    newTableModelMetrics.addColumn("Recall");
                    newTableModelMetrics.addColumn("F-measure");
                    newTableModelMetrics.addColumn("Kappa");
                    newTableModelMetrics.addColumn("MCC");
                    newTableModelMetrics.addColumn("AUC");
                    newTableModelMetrics.addColumn("MAE");
                    newTableModelMetrics.addColumn("MSE");
                    newTableModelMetrics.addColumn("RMSE");
                    newTableModelMetrics.addColumn("MAPE");
                    newTableModelMetrics.addColumn("R2");
                    
                    //load table metrics
                    for(int id = 0; id < ids.size(); id++){
                        ResultSet rs4 = st.executeQuery("SELECT * FROM metrics WHERE idexperiment = " + ids.get(id));                        
                        
                        while(rs4.next()){
                            Object[] fila = new Object[newTableModelMetrics.getColumnCount()];
                            
                            for(int f = 0; f < fila.length; f++) {
                                fila[f] = rs4.getString(f+3);
                            }

                            newTableModelMetrics.addRow(fila);
                        }
                    }
                    
                    metricsTableModel = newTableModelMetrics;
                    metricsTable.setModel(metricsTableModel);
                         
                    checkLoadData("BBDD");
                }
            } catch (SQLException sqle) { 
                m_Log.logMessage("Error in the execution:" + sqle.getErrorCode() + " " + sqle.getMessage());  
                m_Log.statusMessage("See error log");
            }
        } catch (SQLException ex) {
            m_Log.logMessage(ex.getMessage());
            m_Log.statusMessage("See error log");
        } 
    }
    
    private void resetTabResult(){
        //Jpanel predictions
        predictionsTableModel.setRowCount(0);
        predictionsTableModel.setColumnCount(6);
        datasetsListModel.removeAllElements();
        evaluatorListModel.removeAllElements();
        searchListModel.removeAllElements();
        classifierListModel.removeAllElements();
        attributesTextField.setText("");
        captionTableModel.setRowCount(0);
        captionEV.clear();
        captionSE.clear();
        captionCL.clear();
        statisticalTestTableModel.setRowCount(0);
        ropeTextField.setText("1");
        
        //Jpanel metrics
        metricsTableModel.setRowCount(0);
        metricsTableModel.setColumnCount(5);
        accuracyMetricsCheckBox.setSelected(false);
        precisionMetricsCheckBox.setSelected(false);
        recallMetricsCheckBox.setSelected(false);
        fMeasureMetricsCheckBox.setSelected(false);
        kappaMetricsCheckBox.setSelected(false);
        mccMetricsCheckBox.setSelected(false);
        aucMetricsCheckBox.setSelected(false);
        maeMetricsCheckBox.setSelected(false);
        mseMetricsCheckBox.setSelected(false);
        rmseMetricsCheckBox.setSelected(false);
        mapeMetricsCheckBox.setSelected(false);
        r2MetricsCheckBox.setSelected(false);
        
        //Jpanel graph
        metricGraphComboBox.removeAllItems();
        xAxis1ComboBox.removeAllItems();
        xAxis2ComboBox.removeAllItems();
        barChartPanel.removeAll();
    }
    
    public void evaluatorAndSearchPanels() {
        m_AttributeEvaluatorEditor.setClassType(ASEvaluation.class);
        m_AttributeEvaluatorEditor.setValue(ExplorerDefaults.getASEvaluator());
        m_AttributeEvaluatorEditor
                .addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent e) {
                        if (m_AttributeEvaluatorEditor.getValue() instanceof AttributeEvaluator) {
                            if (!(m_AttributeSearchEditor.getValue() instanceof Ranker)) {
                                Object backup = m_AttributeEvaluatorEditor.getBackup();
                                int result
                                        = JOptionPane.showConfirmDialog(null,
                                                "You must use use the Ranker search method "
                                                + "in order to use\n"
                                                + m_AttributeEvaluatorEditor.getValue().getClass()
                                                        .getName()
                                                + ".\nShould I select the Ranker search method for you?",
                                                "Alert!", JOptionPane.YES_NO_OPTION);
                                if (result == JOptionPane.YES_OPTION) {
                                    m_AttributeSearchEditor.setValue(new Ranker());
                                } else {
                                    // restore to what was there previously (if possible)
                                    if (backup != null) {
                                        m_AttributeEvaluatorEditor.setValue(backup);
                                    }
                                }
                            }
                        } else {
                            if (m_AttributeSearchEditor.getValue() instanceof Ranker) {
                                Object backup = m_AttributeEvaluatorEditor.getBackup();
                                int result
                                        = JOptionPane
                                                .showConfirmDialog(
                                                        null,
                                                        "You must use use a search method that explores \n"
                                                        + "the space of attribute subsets (such as GreedyStepwise) in "
                                                        + "order to use\n"
                                                        + m_AttributeEvaluatorEditor.getValue().getClass()
                                                                .getName()
                                                        + ".\nShould I select the GreedyStepwise search method for "
                                                        + "you?\n(you can always switch to a different method afterwards)",
                                                        "Alert!", JOptionPane.YES_NO_OPTION);
                                if (result == JOptionPane.YES_OPTION) {
                                    m_AttributeSearchEditor.setValue(new weka.attributeSelection.GreedyStepwise());
                                } else {
                                    // restore to what was there previously (if possible)
                                    if (backup != null) {
                                        m_AttributeEvaluatorEditor.setValue(backup);
                                    }
                                }
                            }
                        }
                        updateRadioLinks();

                        // check capabilities...
                        Capabilities currentFilter
                                = m_AttributeEvaluatorEditor.getCapabilitiesFilter();
                        ASEvaluation evaluator
                                = (ASEvaluation) m_AttributeEvaluatorEditor.getValue();
                        Capabilities currentSchemeCapabilities = null;
                        if (evaluator != null && currentFilter != null
                                && (evaluator instanceof CapabilitiesHandler)) {
                            currentSchemeCapabilities
                                    = ((CapabilitiesHandler) evaluator).getCapabilities();

                            if (!currentSchemeCapabilities.supportsMaybe(currentFilter)
                                    && !currentSchemeCapabilities.supports(currentFilter)) {
                                runExpBtn.setEnabled(false);
                            }
                        }
                        repaint();
                    }
                });

        m_AttributeSearchEditor.setClassType(ASSearch.class);
        m_AttributeSearchEditor.setValue(ExplorerDefaults.getASSearch());
        m_AttributeSearchEditor
                .addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent e) {
                        if (m_AttributeSearchEditor.getValue() instanceof Ranker) {
                            if (!(m_AttributeEvaluatorEditor.getValue() instanceof AttributeEvaluator)) {
                                Object backup = m_AttributeSearchEditor.getBackup();
                                int result
                                        = JOptionPane
                                                .showConfirmDialog(
                                                        null,
                                                        "You must use use an evaluator that evaluates\n"
                                                        + "single attributes (such as InfoGain) in order to use\n"
                                                        + "the Ranker. Should I select the InfoGain evaluator "
                                                        + "for you?\n"
                                                        + "(You can always switch to a different method afterwards)",
                                                        "Alert!", JOptionPane.YES_NO_OPTION);
                                if (result == JOptionPane.YES_OPTION) {
                                    m_AttributeEvaluatorEditor
                                            .setValue(new weka.attributeSelection.InfoGainAttributeEval());
                                } else {
                                    // restore to what was there previously (if possible)
                                    if (backup != null) {
                                        m_AttributeSearchEditor.setValue(backup);
                                    }
                                }
                            }
                        } else {
                            if (m_AttributeEvaluatorEditor.getValue() instanceof AttributeEvaluator) {
                                Object backup = m_AttributeSearchEditor.getBackup();
                                int result
                                        = JOptionPane
                                                .showConfirmDialog(
                                                        null,
                                                        "You must use use an evaluator that evaluates\n"
                                                        + "subsets of attributes (such as CFS) in order to use\n"
                                                        + m_AttributeEvaluatorEditor.getValue().getClass()
                                                                .getName()
                                                        + ".\nShould I select the CFS subset evaluator for you?"
                                                        + "\n(you can always switch to a different method afterwards)",
                                                        "Alert!", JOptionPane.YES_NO_OPTION);

                                if (result == JOptionPane.YES_OPTION) {
                                    m_AttributeEvaluatorEditor
                                            .setValue(new weka.attributeSelection.CfsSubsetEval());
                                } else {
                                    // restore to what was there previously (if possible)
                                    if (backup != null) {
                                        m_AttributeSearchEditor.setValue(backup);
                                    }
                                }
                            }
                        }
                        repaint();
                    }
                });
    }

    public void classifierPanel() {
        m_ClassifierEditor.setClassType(Classifier.class);
        m_ClassifierEditor.setValue(ExplorerDefaults.getClassifier());
        m_ClassifierEditor.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                // Check capabilities
                Capabilities currentFilter = m_ClassifierEditor.getCapabilitiesFilter();
                Classifier classifier = (Classifier) m_ClassifierEditor.getValue();
                Capabilities currentSchemeCapabilities = null;
                if (classifier != null && currentFilter != null
                        && (classifier instanceof CapabilitiesHandler)) {
                    currentSchemeCapabilities
                            = ((CapabilitiesHandler) classifier).getCapabilities();

                    if (!currentSchemeCapabilities.supportsMaybe(currentFilter)
                            && !currentSchemeCapabilities.supports(currentFilter)) {
                        runExpBtn.setEnabled(false);
                    }
                }
                repaint();
            }
        });
    }

    /**
     * Updates the enabled status of the input fields and labels.
     */
    protected void updateRadioLinks() {
        holdOutSplitBtn.setEnabled(true);
        holdOutSplitTextField.setEnabled(holdOutSplitBtn.isSelected());
        holdOutSplitLabel.setEnabled(holdOutSplitBtn.isSelected());
        crossValidationBtn.setEnabled(true);
        crossValidationTextField.setEnabled(crossValidationBtn.isSelected());
        crossValidationLabel.setEnabled(crossValidationBtn.isSelected());
        leaveOneOutBtn.setEnabled(true);
    }
    
    protected void startAttributeSelectionExp() throws InterruptedException, ExecutionException {
        m_AEEPanel.addToHistory();
        m_ASEPanel.addToHistory();
        m_CEPanel.addToHistory();
        
        m_Log.logMessage("Start of experimentation");
        
        int numThreads = 4;
        
        if(!numThreadsTextField.getText().trim().equals("")){
            try{
                numThreads = Integer.parseInt(numThreadsTextField.getText().trim());
            } catch(NumberFormatException ex){
                m_Log.logMessage(ex.getMessage());
            }
        }
        
        executor = Executors.newFixedThreadPool(numThreads);
        Collection<SearchEvaluatorAndClassifier> concurrentExp = new LinkedList<>();
        Random random;
        Instances train, test = null;
        
        if(!listEvaluators.isEmpty() && !listSearchAlgorithms.isEmpty()){
            for(int i = 0; i < listInstances.size(); i++){
                //listEvaluators and listSearchAlgorithms are the same size
                for(int j = 0; j < listEvaluators.size(); j++){
                    for(int z = 0; z < listClassifier.size(); z++){
                        Instances inst = new Instances(listInstances.get(i));

                        //testMode = 0 holdOutSplit, 1 = crossValidation, 2 = leaveOneOut
                        int testMode = 0;
                        int numFolds = 10;
                        int seed = 1;
                        int classIndex = listInstances.get(i).classIndex();
                        double percent = 70;

                        ASEvaluation evaluator = (ASEvaluation) listEvaluators.get(j);
                        ASSearch search = (ASSearch) listSearchAlgorithms.get(j);
                        //Classifier
                        Classifier cls = (Classifier) listClassifier.get(z);
                        
                        try {
                            if (holdOutSplitBtn.isSelected()) {
                                testMode = 0;
                                percent = Double.parseDouble(holdOutSplitTextField.getText());

                                if ((percent <= 0) || (percent >= 100)) {
                                    throw new Exception("Percentage must be between 0 and 100");
                                }
                                
                                if(!preserveOrderCheckbox.isSelected()){
                                    random = new Random(seed);
                                    inst.randomize(random);
                                }

                                int trainSize = (int) Math.round(inst.numInstances() * percent / 100);
                                int testSize = inst.numInstances() - trainSize;

                                //build train and test
                                train = new Instances(inst, 0, trainSize);
                                test = new Instances(inst, trainSize, testSize);
                                
                                concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, seed, classIndex, 
                                    evaluator, search, cls, progressExp, 0, train, test, 0));
                            }else if (crossValidationBtn.isSelected()) {
                                testMode = 1;
                                numFolds = Integer.parseInt(crossValidationTextField.getText());
                                if (numFolds <= 1) {
                                    throw new Exception("Number of folds must be greater than 1");
                                }
                                
                                random = new Random(seed);
                                inst.randomize(random);

                                //cross-validate classifier                   
                                CrossValidation cv = new CrossValidation(inst, numFolds);
                                int fold = 1;

                                while(cv.hasNext()){
                                    Instances[] trainTest = cv.next();

                                    test = trainTest[1];
                                    train = trainTest[0];

                                    
                                    concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, seed, classIndex, 
                                        evaluator, search, cls, progressExp, 0, train, test, fold));
                                    fold++;
                                } 
                            }else if (leaveOneOutBtn.isSelected()) {
                                testMode = 2;
                                
                                random = new Random(seed);
                                inst.randomize(random);
 
                                concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, seed, classIndex, 
                                    evaluator, search, cls, progressExp, 0, null, null, 1));
                            }
                           
                            train = null;
                            test = null;

                            System.gc();
                        } catch (Exception ex) {
                            m_Log.logMessage(ex.getMessage());
                            m_Log.statusMessage("See error log");
                        }
                    }
                }
            }
        }
        
        //no attribute selection
        for(int i = 0; i < listInstances.size(); i++){
            for(int j = 0; j < listClassifier.size(); j++){
                Instances inst = new Instances(listInstances.get(i));
                //Classifier
                Classifier cls = (Classifier) listClassifier.get(j);
                
                //testMode = 0 holdOutSplit, 1 = crossValidation, 2 = leaveOneOut
                int testMode = 0;
                int numFolds = 10;
                int seed = 1;
                int classIndex = listInstances.get(i).classIndex();
                double percent = 70;

                try {
                    if (holdOutSplitBtn.isSelected()) {
                        testMode = 0;
                        percent = Double.parseDouble(holdOutSplitTextField.getText());

                        if ((percent <= 0) || (percent >= 100)) {
                            throw new Exception("Percentage must be between 0 and 100");
                        }
                        
                        if(!preserveOrderCheckbox.isSelected()){
                            random = new Random(seed);
                            inst.randomize(random);
                        }

                        int trainSize = (int) Math.round(inst.numInstances() * percent / 100);
                        int testSize = inst.numInstances() - trainSize;

                        //build train and test
                        train = new Instances(inst, 0, trainSize);
                        test = new Instances(inst, trainSize, testSize);

                        concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, seed, classIndex, 
                            null, null, cls, progressExp, 0, train, test, 0));
                    }else if (crossValidationBtn.isSelected()) {
                        testMode = 1;
                        numFolds = Integer.parseInt(crossValidationTextField.getText());
                        if (numFolds <= 1) {
                            throw new Exception("Number of folds must be greater than 1");
                        }
                        
                        random = new Random(seed);
                        inst.randomize(random);

                        //cross-validate classifier                   
                        CrossValidation cv = new CrossValidation(inst, numFolds);
                        int fold = 1;

                        while(cv.hasNext()){
                            Instances[] trainTest = cv.next();

                            test = trainTest[1];
                            train = trainTest[0];


                            concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, seed, classIndex, 
                                null, null, cls, progressExp, 0, train, test, fold));
                            fold++;
                        } 
                    }else if (leaveOneOutBtn.isSelected()) {
                        testMode = 2;
                                
                        random = new Random(seed);
                        inst.randomize(random);

                        concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, seed, classIndex, 
                            null, null, cls, progressExp, 0, null, null, 1));
                    }
                } catch (Exception ex) {
                    m_Log.logMessage(ex.getMessage());
                    m_Log.statusMessage("See error log");
                }
            }
        }
        
        for (SearchEvaluatorAndClassifier t : concurrentExp) {
            t.setNumTareas(concurrentExp.size());
        }
        
        resultsAttrSelExp = new LinkedList<Future<ResultsAttrSelExp>>();
        
        try {
            resultsAttrSelExp = executor.invokeAll(concurrentExp);
        } catch (InterruptedException ex) {
            m_Log.logMessage(ex.getMessage());
        }
        
        executor.shutdown();
        
        //so that it reaches 100% regardless of what is assigned to each thread
        /*For example, with 12 objects, each thread would have an assigned 8%, 
        at the end it would be 96 not 100, because of the decimals*/
        if((resultsAttrSelExp.size() == concurrentExp.size()) && (progressExp.getValue() < 100)){
            progressExp.setValue(100);
        }else if(resultsAttrSelExp.size() != concurrentExp.size()){
            progressExp.setValue(0);
        }

        concurrentExp = null;
                
        System.gc();
        
        checkLoadData("results");
        runExpBtn.setEnabled(true);
        stopExpBtn.setEnabled(false);
        m_Log.logMessage("Finished all threads");
        completeTabResult();
    }

    @Override
    public void setExplorer(Explorer parent) {
        m_Explorer = parent;
    }

    @Override
    public Explorer getExplorer() {
        return m_Explorer;
    }

    @Override
    public void setInstances(Instances inst) {
        m_Instances = inst;
    }

    @Override
    public String getTabTitle() {
        return "Feature Selection Studio";
    }

    @Override
    public String getTabTitleToolTip() {
        return "Selection attributes of instances";
    }
    
    @Override
    public void setLog(weka.gui.Logger logger) {
        m_Log = logger;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox accuracyMetricsCheckBox;
    private javax.swing.JLabel actionsClassifierLabel;
    private javax.swing.JLabel actionsFeatureLabel;
    private javax.swing.JButton addClassifierBtn;
    private javax.swing.JButton addFeatureBtn;
    private javax.swing.JButton addFileBtn;
    private javax.swing.JButton addFolderBtn;
    private javax.swing.JTabbedPane attrSelExpTabs;
    private javax.swing.JLabel attributesLabel;
    private javax.swing.JTextField attributesTextField;
    private javax.swing.JCheckBox aucMetricsCheckBox;
    private javax.swing.JPanel barChartPanel;
    private javax.swing.JScrollPane captionScrollPane;
    private javax.swing.JTable captionTable;
    private javax.swing.JLabel classDatasetsLabel;
    private javax.swing.JLabel classificationMetricsLabel;
    private javax.swing.JPanel classifier;
    private javax.swing.JLabel classifierLabel;
    private javax.swing.JList<String> classifierList;
    private javax.swing.JScrollPane classifierListScrollPane;
    private javax.swing.JScrollPane classifierScrollPane;
    private javax.swing.JPanel classifierSelectionPanel;
    private javax.swing.JTable classifierTable;
    private javax.swing.JLabel classifiersLabel;
    private javax.swing.JRadioButton crossValidationBtn;
    private javax.swing.JLabel crossValidationLabel;
    private javax.swing.JTextField crossValidationTextField;
    private javax.swing.JPanel datasets;
    private javax.swing.JLabel datasetsLabel;
    private javax.swing.JList<String> datasetsList;
    private javax.swing.JScrollPane datasetsListScrollPane;
    private javax.swing.JScrollPane datasetsScrollPane;
    private javax.swing.JTable datasetsTable;
    private javax.swing.JLabel evaluatorLabel;
    private javax.swing.JList<String> evaluatorList;
    private javax.swing.JScrollPane evaluatorListScrollPane;
    private javax.swing.JPanel evaluatorPanel;
    private javax.swing.JLabel evaluatorsLabel;
    private javax.swing.JPanel experiment;
    private javax.swing.JPanel experimentPanel;
    private javax.swing.JCheckBox fMeasureMetricsCheckBox;
    private javax.swing.JScrollPane featureScrollPane;
    private javax.swing.JPanel featureSelection;
    private javax.swing.JTable featuresTable;
    private javax.swing.JComboBox<String> filterGraph1ComboBox;
    private javax.swing.JComboBox<String> filterGraph2ComboBox;
    private javax.swing.JComboBox<String> filterStatisComboBox;
    private javax.swing.JLabel filterStatisLabel;
    private javax.swing.JPanel graph;
    private javax.swing.JLabel groupsLOOLabel;
    private javax.swing.JRadioButton holdOutSplitBtn;
    private javax.swing.JLabel holdOutSplitLabel;
    private javax.swing.JTextField holdOutSplitTextField;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JCheckBox kappaMetricsCheckBox;
    private javax.swing.JLabel labelFilter1;
    private javax.swing.JLabel labelFilter2;
    private javax.swing.JRadioButton leaveOneOutBtn;
    private javax.swing.JButton loadBBDDBtn;
    private javax.swing.JButton loadBtnClassifier;
    private javax.swing.JButton loadBtnDataset;
    private javax.swing.JButton loadBtnFeature;
    private javax.swing.JButton loadDatasetMetricsBtn;
    private javax.swing.JButton loadDatasetPredictionsBtn;
    private javax.swing.JButton loadExpBtn;
    private javax.swing.JCheckBox maeMetricsCheckBox;
    private javax.swing.JCheckBox mapeMetricsCheckBox;
    private javax.swing.JCheckBox mccMetricsCheckBox;
    private javax.swing.JLabel messageNormTestLabel;
    private javax.swing.JLabel messagesStatis;
    private javax.swing.JComboBox<String> metricGraphComboBox;
    private javax.swing.JLabel metricGraphLabel;
    private javax.swing.JComboBox<String> metricStatisComboBox;
    private javax.swing.JLabel metricStatisLabel;
    private javax.swing.JPanel metrics;
    private javax.swing.JScrollPane metricsScrollPane;
    private javax.swing.JTable metricsTable;
    private javax.swing.JCheckBox mseMetricsCheckBox;
    private javax.swing.JLabel nameExpLabel;
    private javax.swing.JTextField nameExpTextField;
    private javax.swing.JPanel nomenclaturePanel;
    private javax.swing.JLabel numDatasetsStatisLabel;
    private javax.swing.JLabel numDatasetsStatisValue;
    private javax.swing.JLabel numSamStatisLabel;
    private javax.swing.JLabel numSamStatisValue;
    private javax.swing.JLabel numThreadsLabel;
    private javax.swing.JTextField numThreadsTextField;
    private javax.swing.JLabel positiveClassDatasetsLabel;
    private javax.swing.JCheckBox precisionMetricsCheckBox;
    private javax.swing.JPanel predictions;
    private javax.swing.JScrollPane predictionsScrollPane;
    private javax.swing.JTable predictionsTable;
    private javax.swing.JCheckBox preserveOrderCheckbox;
    private javax.swing.JProgressBar progressExp;
    private javax.swing.JButton putPreprocessStatisBtn;
    private javax.swing.JCheckBox r2MetricsCheckBox;
    private javax.swing.JCheckBox recallMetricsCheckBox;
    private javax.swing.JLabel regressionMetricsLabel;
    private javax.swing.JButton removeBtnClassifier;
    private javax.swing.JButton removeBtnDataset;
    private javax.swing.JButton removeBtnFeature;
    private javax.swing.JPanel results;
    private javax.swing.JCheckBox rmseMetricsCheckBox;
    private javax.swing.JLabel ropeLabel;
    private javax.swing.JTextField ropeTextField;
    private javax.swing.JButton runExpBtn;
    private javax.swing.JPanel runPanel;
    private javax.swing.JButton saveARFFMetricsBtn;
    private javax.swing.JButton saveARFFPredictionsBtn;
    private javax.swing.JButton saveBBDDBtn;
    private javax.swing.JButton saveCSVMetricsBtn;
    private javax.swing.JButton saveCSVPredictionsBtn;
    private javax.swing.JButton saveCSVStatisBtn;
    private javax.swing.JButton saveExpBtn;
    private javax.swing.JButton saveLatexMetricsBtn;
    private javax.swing.JButton savePDFGraphBtn;
    private javax.swing.JButton savePNGGraphBtn;
    private javax.swing.JLabel searchLabel;
    private javax.swing.JList<String> searchList;
    private javax.swing.JScrollPane searchListScrollPane;
    private javax.swing.JPanel searchPanel;
    private javax.swing.JLabel searchsLabel;
    private javax.swing.JComboBox<String> selectionClassCombo;
    private javax.swing.JComboBox<String> selectionPositiveClassCombo;
    private javax.swing.JScrollPane statisticalTestScrollPane;
    private javax.swing.JTable statisticalTestTable;
    private javax.swing.JPanel statisticalTestsPanel;
    private javax.swing.JButton stopExpBtn;
    private javax.swing.JButton updateGraphBtn;
    private javax.swing.JButton updatePredictionsBtn;
    private javax.swing.JPanel validation;
    private javax.swing.ButtonGroup validationRadioBtn;
    private javax.swing.JTextField valueGroupsLOOTextField;
    private javax.swing.JComboBox<String> xAxis1ComboBox;
    private javax.swing.JLabel xAxis1Label;
    private javax.swing.JComboBox<String> xAxis2ComboBox;
    private javax.swing.JLabel xAxis2Label;
    // End of variables declaration//GEN-END:variables
}
