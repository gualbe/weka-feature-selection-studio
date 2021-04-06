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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
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
import weka.core.converters.ConverterUtils;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.DatabaseSaver;

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
    protected List<Instances> listIntances;

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
        featuresTableModel = (DefaultTableModel) featuresTable.getModel();
        classifierTableModel = (DefaultTableModel) classifierTable.getModel();
        predictionsTableModel = (DefaultTableModel) predictionsTable.getModel();
        metricsTableModel = (DefaultTableModel) metricsTable.getModel();
        
        //lists of the Jpanel
        datasetsList.setModel(datasetsListModel);
        evaluatorList.setModel(evaluatorListModel);
        searchList.setModel(searchListModel);
        classifierList.setModel(classifierListModel);
        
        //other attributes for save things
        listIntances = new ArrayList<>();
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
        datasetsScrollPane = new javax.swing.JScrollPane();
        datasetsTable = new javax.swing.JTable();
        actionsDatasetsLabel = new javax.swing.JLabel();
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
        runPanel = new javax.swing.JPanel();
        progressExp = new javax.swing.JProgressBar();
        runExpBtn = new javax.swing.JButton();
        stopExpBtn = new javax.swing.JButton();
        numThreadsLabel = new javax.swing.JLabel();
        numThreadsTextField = new javax.swing.JTextField();
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
        attributesLabel = new javax.swing.JLabel();
        attributesTextField = new javax.swing.JTextField();
        saveCSVPredictionsBtn = new javax.swing.JButton();
        saveARFFPredictionsBtn = new javax.swing.JButton();
        loadDatasetPredictionsBtn = new javax.swing.JButton();
        updatePredictionsBtn = new javax.swing.JButton();
        datasetsPredictionsLabel = new javax.swing.JLabel();
        evaluatorsPredictionsLabel = new javax.swing.JLabel();
        searchsPredictionsLabel = new javax.swing.JLabel();
        classifiersPredictionsLabel = new javax.swing.JLabel();
        saveBBDDBtn = new javax.swing.JButton();
        loadBBDDBtn = new javax.swing.JButton();
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
        gridBagConstraints.weightx = 1.0;
        datasets.add(addFileBtn, gridBagConstraints);

        datasetsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Relation", "Instances", "Attributes", "Number classes", "Class", "Positive class"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class
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
        datasetsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                datasetsTableMouseClicked(evt);
            }
        });
        datasetsScrollPane.setViewportView(datasetsTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        datasets.add(datasetsScrollPane, gridBagConstraints);

        actionsDatasetsLabel.setText("Actions: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        datasets.add(actionsDatasetsLabel, gridBagConstraints);

        loadBtnDataset.setText("Load");
        loadBtnDataset.setToolTipText("Load the selected row of the table in the tab Preprocess");
        loadBtnDataset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadBtnDatasetActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
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
        gridBagConstraints.gridx = 3;
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
        gridBagConstraints.gridx = 5;
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
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        datasets.add(selectionPositiveClassCombo, gridBagConstraints);

        classDatasetsLabel.setText("Class: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        datasets.add(classDatasetsLabel, gridBagConstraints);

        positiveClassDatasetsLabel.setText("Positive class: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
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

classifierLabel.setText("Classifier:");
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
    gridBagConstraints.gridy = 2;
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
    gridBagConstraints.gridy = 1;
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
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(stopExpBtn, gridBagConstraints);

    numThreadsLabel.setText("Threads:");
    numThreadsLabel.setToolTipText("Number of threads with which it will be executed");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(numThreadsLabel, gridBagConstraints);

    numThreadsTextField.setToolTipText("Number of threads with which it will be executed");
    numThreadsTextField.setMinimumSize(new java.awt.Dimension(20, 22));
    numThreadsTextField.setPreferredSize(new java.awt.Dimension(50, 22));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    runPanel.add(numThreadsTextField, gridBagConstraints);

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
            "Actual value", "Predicted value", "Dataset", "Evaluator", "Search", "Classifier"
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
    gridBagConstraints.gridwidth = 6;
    gridBagConstraints.gridheight = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    predictions.add(predictionsScrollPane, gridBagConstraints);

    datasetsListScrollPane.setMinimumSize(new java.awt.Dimension(110, 23));
    datasetsListScrollPane.setName(""); // NOI18N
    datasetsListScrollPane.setPreferredSize(new java.awt.Dimension(110, 23));

    datasetsListScrollPane.setViewportView(datasetsList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(datasetsListScrollPane, gridBagConstraints);

    evaluatorListScrollPane.setMinimumSize(new java.awt.Dimension(110, 23));
    evaluatorListScrollPane.setViewportView(evaluatorList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(evaluatorListScrollPane, gridBagConstraints);

    searchListScrollPane.setMinimumSize(new java.awt.Dimension(110, 23));

    searchListScrollPane.setViewportView(searchList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(searchListScrollPane, gridBagConstraints);

    classifierListScrollPane.setMinimumSize(new java.awt.Dimension(110, 23));

    classifierListScrollPane.setViewportView(classifierList);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    predictions.add(classifierListScrollPane, gridBagConstraints);

    attributesLabel.setText("Attributes:");
    attributesLabel.setToolTipText("The input format must be number followed by like, eg: 1, 2");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 3;
    predictions.add(attributesLabel, gridBagConstraints);

    attributesTextField.setToolTipText("The input format must be number followed by like, eg: 1, 2");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(attributesTextField, gridBagConstraints);

    saveCSVPredictionsBtn.setText("Save to CSV");
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
    saveARFFPredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveARFFPredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    predictions.add(saveARFFPredictionsBtn, gridBagConstraints);

    loadDatasetPredictionsBtn.setText("Load dataset");
    loadDatasetPredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadDatasetPredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    predictions.add(loadDatasetPredictionsBtn, gridBagConstraints);

    updatePredictionsBtn.setText("Update");
    updatePredictionsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            updatePredictionsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(updatePredictionsBtn, gridBagConstraints);

    datasetsPredictionsLabel.setText("Datasets");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    predictions.add(datasetsPredictionsLabel, gridBagConstraints);

    evaluatorsPredictionsLabel.setText("Evaluators");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 0;
    predictions.add(evaluatorsPredictionsLabel, gridBagConstraints);

    searchsPredictionsLabel.setText("Searchs");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    predictions.add(searchsPredictionsLabel, gridBagConstraints);

    classifiersPredictionsLabel.setText("Classifiers");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 0;
    predictions.add(classifiersPredictionsLabel, gridBagConstraints);

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
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    predictions.add(loadBBDDBtn, gridBagConstraints);

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
    metricsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    metricsScrollPane.setViewportView(metricsTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.gridheight = 9;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    metrics.add(metricsScrollPane, gridBagConstraints);

    saveCSVMetricsBtn.setText("Save to CSV");
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
    saveARFFMetricsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveARFFMetricsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    metrics.add(saveARFFMetricsBtn, gridBagConstraints);

    loadDatasetMetricsBtn.setText("Load dataset");
    loadDatasetMetricsBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            loadDatasetMetricsBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(loadDatasetMetricsBtn, gridBagConstraints);

    classificationMetricsLabel.setText("Classification");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    metrics.add(classificationMetricsLabel, gridBagConstraints);

    regressionMetricsLabel.setText("Regression");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 3;
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
    gridBagConstraints.gridx = 4;
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
    gridBagConstraints.gridx = 4;
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
    gridBagConstraints.gridx = 4;
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
    gridBagConstraints.gridx = 4;
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
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    metrics.add(r2MetricsCheckBox, gridBagConstraints);

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
    gridBagConstraints.gridheight = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    graph.add(barChartPanel, gridBagConstraints);

    metricGraphLabel.setText("Metric:");
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
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    graph.add(xAxis2ComboBox, gridBagConstraints);

    savePDFGraphBtn.setText("Save to PDF");
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
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    graph.add(savePDFGraphBtn, gridBagConstraints);

    savePNGGraphBtn.setText("Save to PNG");
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
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    graph.add(savePNGGraphBtn, gridBagConstraints);

    updateGraphBtn.setText("Update");
    updateGraphBtn.setMaximumSize(new java.awt.Dimension(85, 23));
    updateGraphBtn.setMinimumSize(new java.awt.Dimension(85, 21));
    updateGraphBtn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            updateGraphBtnActionPerformed(evt);
        }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.weighty = 1.0;
    graph.add(updateGraphBtn, gridBagConstraints);

    graph.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder("Graph"),
        BorderFactory.createEmptyBorder(0, 5, 5, 5)));

gridBagConstraints = new java.awt.GridBagConstraints();
gridBagConstraints.gridx = 1;
gridBagConstraints.gridy = 1;
gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
gridBagConstraints.weightx = 1.0;
gridBagConstraints.weighty = 1.0;
results.add(graph, gridBagConstraints);

attrSelExpTabs.addTab("tab2", results);

add(attrSelExpTabs, java.awt.BorderLayout.CENTER);
attrSelExpTabs.addTab("Experiment", experiment);
attrSelExpTabs.addTab("Results", results);
}// </editor-fold>//GEN-END:initComponents

    private void addFileBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFileBtnActionPerformed
        m_Instances.setClassIndex(m_Instances.numAttributes() - 1);
                
        datasetsTableModel.addRow(new Object[]{m_Instances.relationName(), m_Instances.numInstances(), 
            m_Instances.numAttributes() - 1, m_Instances.numClasses(), m_Instances.attribute(m_Instances.numAttributes() - 1).name(), 
            m_Instances.attribute(m_Instances.classIndex()).value(0)});
        
        listIntances.add(m_Instances);
        listClassPositive.add(0);
        
        checkRun();
    }//GEN-LAST:event_addFileBtnActionPerformed

    private void removeBtnDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBtnDatasetActionPerformed
        listIntances.remove(datasetsTable.getSelectedRow());
        listClassPositive.remove(datasetsTable.getSelectedRow());
        datasetsTableModel.removeRow(datasetsTable.getSelectedRow());
        checkRun();
    }//GEN-LAST:event_removeBtnDatasetActionPerformed

    private void loadBtnDatasetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBtnDatasetActionPerformed
        getExplorer().getPreprocessPanel().setInstances(listIntances.get(datasetsTable.getSelectedRow()));
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
        listEvaluators.remove(featuresTable.getSelectedRow());
        listSearchAlgorithms.remove(featuresTable.getSelectedRow());
        featuresTableModel.removeRow(featuresTable.getSelectedRow());
        checkRun();
    }//GEN-LAST:event_removeBtnFeatureActionPerformed

    private void loadBtnFeatureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBtnFeatureActionPerformed
        m_AttributeSearchEditor.setValue(listSearchAlgorithms.get(featuresTable.getSelectedRow()));
        m_AttributeEvaluatorEditor.setValue(listEvaluators.get(featuresTable.getSelectedRow()));
    }//GEN-LAST:event_loadBtnFeatureActionPerformed

    private void loadBtnClassifierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBtnClassifierActionPerformed
        m_ClassifierEditor.setValue(listClassifier.get(classifierTable.getSelectedRow()));
    }//GEN-LAST:event_loadBtnClassifierActionPerformed

    private void removeBtnClassifierActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBtnClassifierActionPerformed
        listClassifier.remove(classifierTable.getSelectedRow());
        classifierTableModel.removeRow(classifierTable.getSelectedRow());
        checkRun();
    }//GEN-LAST:event_removeBtnClassifierActionPerformed

    private void datasetsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_datasetsTableMouseClicked
        Instances inst = listIntances.get(datasetsTable.getSelectedRow());
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
        Instances inst = listIntances.get(datasetsTable.getSelectedRow());

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
                    cls = getSpec(objectResult.get().getClassifier());
                    eV = "original";
                    search = "original";

                    if(objectResult.get().getEvaluator() != null){
                        eV = getSpec(objectResult.get().getEvaluator());
                        search = getSpec(objectResult.get().getSearch());

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
        Instances inst = listIntances.get(datasetsTable.getSelectedRow());
        
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
                DefaultCategoryDataset result = new DefaultCategoryDataset();
                String metricSelected = (String)metricGraphComboBox.getSelectedItem();
                String xAxis1 = (String)xAxis1ComboBox.getSelectedItem();
                String xAxis2 = (String)xAxis2ComboBox.getSelectedItem();
                List dataMetrics = new ArrayList();

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

                for(int i = 0; i < dataMetrics.size(); i++){
                    Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);

                    //if not applicable or an error has occurred
                    if(dataMetrics.get(i) != null){
                        try {
                            if(xAxis1.equals("Dataset") && xAxis2.equals("Search")){
                                if(objectResult.get().getSearch() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getSearch()), 
                                        objectResult.get().getInst().relationName());
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        "original", 
                                        objectResult.get().getInst().relationName());
                                }
                            } else if(xAxis1.equals("Dataset") && xAxis2.equals("Evaluator")){
                                if(objectResult.get().getEvaluator() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getEvaluator()), 
                                        objectResult.get().getInst().relationName());
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        "original", 
                                        objectResult.get().getInst().relationName());
                                }
                            } else if(xAxis1.equals("Dataset") && xAxis2.equals("Classifier")){
                                result.setValue((double)dataMetrics.get(i), 
                                    getSpec(objectResult.get().getClassifier()), 
                                    objectResult.get().getInst().relationName());
                            } else if(xAxis1.equals("Search") && xAxis2.equals("Dataset")){
                                if(objectResult.get().getSearch() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        objectResult.get().getInst().relationName(),
                                        getSpec(objectResult.get().getSearch()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        objectResult.get().getInst().relationName(),
                                        "original");
                                }
                            } else if(xAxis1.equals("Search") && xAxis2.equals("Evaluator")){
                                if(objectResult.get().getSearch() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getEvaluator()),
                                        getSpec(objectResult.get().getSearch()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        "original",
                                        "original");
                                }
                            } else if(xAxis1.equals("Search") && xAxis2.equals("Classifier")){
                                if(objectResult.get().getSearch() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getClassifier()),
                                        getSpec(objectResult.get().getSearch()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getClassifier()),
                                        "original");
                                } 
                            } else if(xAxis1.equals("Evaluator") && xAxis2.equals("Dataset")){
                                if(objectResult.get().getEvaluator() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        objectResult.get().getInst().relationName(),
                                        getSpec(objectResult.get().getEvaluator()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        objectResult.get().getInst().relationName(),
                                        "original");
                                }
                            } else if(xAxis1.equals("Evaluator") && xAxis2.equals("Search")){
                                if(objectResult.get().getEvaluator() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getSearch()),
                                        getSpec(objectResult.get().getEvaluator()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        "original",
                                        "original");
                                }
                            } else if(xAxis1.equals("Evaluator") && xAxis2.equals("Classifier")){
                                if(objectResult.get().getEvaluator() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getClassifier()),
                                        getSpec(objectResult.get().getEvaluator()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getClassifier()),
                                        "original");
                                }
                            } else if(xAxis1.equals("Classifier") && xAxis2.equals("Dataset")){
                                result.setValue((double)dataMetrics.get(i), 
                                    objectResult.get().getInst().relationName(),
                                    getSpec(objectResult.get().getClassifier()));
                            } else if(xAxis1.equals("Classifier") && xAxis2.equals("Evaluator")){
                                if(objectResult.get().getEvaluator() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getEvaluator()),
                                        getSpec(objectResult.get().getClassifier()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        "original",
                                        getSpec(objectResult.get().getClassifier()));
                                }
                            } else if(xAxis1.equals("Classifier") && xAxis2.equals("Search")){
                                if(objectResult.get().getSearch() != null){
                                    result.setValue((double)dataMetrics.get(i), 
                                        getSpec(objectResult.get().getSearch()),
                                        getSpec(objectResult.get().getClassifier()));
                                }else{
                                    result.setValue((double)dataMetrics.get(i), 
                                        "original",
                                        getSpec(objectResult.get().getClassifier()));
                                }   
                            }   
                        } catch (InterruptedException | ExecutionException ex) {
                            result.setValue(0, "", "");
                        }
                    }

                }

                chart = ChartFactory.createBarChart3D(null, xAxis1+"/"+xAxis2, metricSelected,result, PlotOrientation.VERTICAL, true, true, false);
                CategoryPlot plot = (CategoryPlot) chart.getPlot();
                plot.setBackgroundAlpha(0.5f);
                ChartPanel chartPanel = new ChartPanel(chart);
                barChartPanel.removeAll();
                barChartPanel.setLayout(new java.awt.BorderLayout());
                barChartPanel.add(chartPanel);   
                barChartPanel.validate();
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

    public static String getSpec(Object object) {
        if (object instanceof OptionHandler) {
            return object.getClass().getSimpleName() + " "
                    + Utils.joinOptions(((OptionHandler)object).getOptions());
        }
        
        return object.getClass().getSimpleName();
    }
    
    private void checkRun() {
        if (!listIntances.isEmpty() && !listClassifier.isEmpty()) {
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
            completePredictionPanel();
            completeMetricsTable();
            completeGraphComboBox();
            JOptionPane.showMessageDialog(null, "The data in the numAttributes column of the metrics table "
                + "is obtained by filtering the entire dataset if the CV or LVO validation method has been chosen.");
        }
    }
    
    private void completePredictionPanel() throws InterruptedException, ExecutionException{
        //complete list of datasets
        for(int d = 0; d < listIntances.size(); d++){
            datasetsListModel.addElement(listIntances.get(d).relationName());
        }
        
        //complete lists of evaluators and searchs, have same size
        for(int e = 0; e < listEvaluators.size(); e++){
            evaluatorListModel.addElement(getSpec(listEvaluators.get(e)));
            searchListModel.addElement(getSpec(listSearchAlgorithms.get(e)));
        }
        
        //no attribute selection
        evaluatorListModel.addElement("original");
        searchListModel.addElement("original");
        
        //complete list of classifiers
        for(int c = 0; c < listClassifier.size(); c++){
            classifierListModel.addElement(getSpec(listClassifier.get(c)));
        }
        
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            String dS = objectResult.get().getInst().relationName();
            String cls = getSpec(objectResult.get().getClassifier());
            String eV = "original";
            String search = "original";
            
            if(objectResult.get().getEvaluator() != null){
                eV = getSpec(objectResult.get().getEvaluator());
                search = getSpec(objectResult.get().getSearch());
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
    
    private void completeMetricsTable() throws InterruptedException, ExecutionException{
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            
            if(objectResult.get().getEvaluator() != null){
                metricsTableModel.addRow(new Object[]{objectResult.get().getInst().relationName(), getSpec(objectResult.get().getEvaluator()),
                    getSpec(objectResult.get().getSearch()), getSpec(objectResult.get().getClassifier()), objectResult.get().getNumAttr()});
            }else{
                metricsTableModel.addRow(new Object[]{objectResult.get().getInst().relationName(), "original",
                "original", getSpec(objectResult.get().getClassifier()), objectResult.get().getNumAttr()});
            }
        }
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
        
        if(listIntances.get(0).classAttribute().isNominal()){
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

        while(j < listIntances.size() && !found){
            if(listIntances.get(j).relationName().equals(instLost.relationName())){
                found = true;
            }
            j++;
        }
                
        return (j-1);
    }
    
    private List accuracy() throws InterruptedException, ExecutionException{
        List accuracy = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                if(evalResult.predictions() != null){
                    try{
                        accuracy.add(evalResult.pctCorrect());
                    } catch(Exception ex){
                        accuracy.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }   
                }else{
                    accuracy.add(null);
                }  
            }else{
                accuracy.add(null);
            }
        }
        
        return accuracy;
    }
    
    private List  precision() throws InterruptedException, ExecutionException{
        List precision = new ArrayList<>();
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

           try {
                if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                    if(evalResult.predictions() != null){
                        try{
                            int index = foundInstances(objectResult.get().getInst());
                            precision.add(evalResult.precision(listClassPositive.get(index)));
                        } catch(Exception ex){
                            precision.add(null);
                            m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                                objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                                objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                                objectResult.get().getClassifier().getClass().getSimpleName() +  
                                " throw the exception: " + ex.getMessage());
                        }     
                    }else{
                        precision.add(null);
                    }
                }else{
                    precision.add(null);
                }
            } catch(InterruptedException | ExecutionException ex){
                precision.add(null);
            }  
        }
        
        return precision;
    }
    
    private List recall() throws InterruptedException, ExecutionException{
        List recall = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                if(evalResult.predictions() != null){
                    try{
                        int index = foundInstances(objectResult.get().getInst());
                        recall.add(evalResult.recall(index));
                    } catch(Exception ex){
                        recall.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }   
                }else{
                    recall.add(null);
                }
            }else{
                recall.add(null);
            }
        }
                
        return recall;
    }
    
    private List fMeasure() throws InterruptedException, ExecutionException{
        List fMeasure = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                if(evalResult.predictions() != null){
                    try{
                        int index = foundInstances(objectResult.get().getInst());
                        fMeasure.add(evalResult.fMeasure(index));
                    } catch(Exception ex){
                        fMeasure.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    fMeasure.add(null);
                }
            }else{
                fMeasure.add(null);
            }
        }
                
        return fMeasure;
    }
    
    private List kappa() throws InterruptedException, ExecutionException{
        List kappa = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                if(evalResult.predictions() != null){
                    try{
                        kappa.add(evalResult.kappa());
                    } catch(Exception ex){
                        kappa.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    kappa.add(null);
                }
            }else{
                kappa.add(null);
            }
        }
                
        return kappa;
    }
    
    private List mcc() throws InterruptedException, ExecutionException{
        List mcc = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                if(evalResult.predictions() != null){
                    try{
                        int index = foundInstances(objectResult.get().getInst());
                        mcc.add(evalResult.matthewsCorrelationCoefficient(index));
                    } catch(Exception ex){
                        mcc.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    mcc.add(null);
                }
            }else{
                mcc.add(null);
            }
        }
                
        return mcc;
    }
    
    private List auc() throws InterruptedException, ExecutionException{
        List auc = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNominal()){
                if(evalResult.predictions() != null){
                    try{
                        int index = foundInstances(objectResult.get().getInst());
                        auc.add(evalResult.areaUnderROC(index));
                    } catch(Exception ex){
                        auc.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    auc.add(null);
                }
            }else{
                auc.add(null);
            }
        }
                
        return auc; 
    }
    
    private List mae() throws InterruptedException, ExecutionException{
        List mae = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNumeric()){
                if(evalResult.predictions() != null){
                    try{
                        mae.add(evalResult.meanAbsoluteError());
                    } catch(Exception ex){
                        mae.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    mae.add(null);
                }
            }else{
                mae.add(null);
            }
        }
                
        return mae; 
    }
    
    private List mse() throws InterruptedException, ExecutionException{
        List mse = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNumeric()){
                if(evalResult.predictions() != null){
                    try{
                        mse.add(Math.pow(evalResult.rootMeanSquaredError(), 2));
                    } catch(Exception ex){
                        mse.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    mse.add(null);
                }
            }else{
                mse.add(null);
            }
        }
                
        return mse;
    }
    
    private List rmse() throws InterruptedException, ExecutionException{
        List rmse = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNumeric()){
                if(evalResult.predictions() != null){
                    try{
                        rmse.add(evalResult.rootMeanSquaredError());
                    } catch(Exception ex){
                        rmse.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    rmse.add(null);
                }
            }else{
                rmse.add(null);
            }
        }
                
        return rmse;
    }
    
    private List mape() throws InterruptedException, ExecutionException{
        List mape = new ArrayList(metricsTableModel.getRowCount());
        int size = 0;

        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            if(evalResult != null && objectResult.get().getInst().classAttribute().isNumeric()){
                if(evalResult.predictions() != null){
                    try{
                        double aux = 0.0;

                        List<Prediction> valuesPredict = evalResult.predictions();

                        for(int z = 0; z < valuesPredict.size(); z++){
                            aux += Math.abs(valuesPredict.get(z).actual() - valuesPredict.get(z).predicted())/valuesPredict.get(z).predicted();
                            size++;
                        }

                        mape.add((aux/size)*100);
                    } catch(Exception ex){
                        mape.add(null);
                        m_Log.logMessage("With " + objectResult.get().getInst().relationName() + ", " + 
                            objectResult.get().getEvaluator().getClass().getSimpleName() + ", " +
                            objectResult.get().getSearch().getClass().getSimpleName() + " and " + 
                            objectResult.get().getClassifier().getClass().getSimpleName() +  
                            " throw the exception: " + ex.getMessage());
                    }
                }else{
                    mape.add(null);
                }
            }else{
                mape.add(null);
            }
        }
                
        return mape;
    }
    
    private List r2() throws InterruptedException, ExecutionException{
        List r2 = new ArrayList(metricsTableModel.getRowCount());
                
        for(int i = 0; i < resultsAttrSelExp.size(); i++){
            Future<ResultsAttrSelExp> objectResult = resultsAttrSelExp.get(i);
            Evaluation evalResult = objectResult.get().getEvalClassifier();

            //if the class is not numeric or there has been an error, throw an exception
            try {
                //if evalResult is null, go to catch
                if(evalResult.predictions() != null){
                    r2.add(evalResult.correlationCoefficient());
                }else{
                    r2.add(null);
                }
            } catch (Exception ex) {
                r2.add(null);
            }
        }
                
        return r2;
    }
    
    private Instances createObjectInstances(DefaultTableModel tableModel, String name){
        ArrayList<Attribute> listAttributes = new ArrayList<>();
        
        for(int i = 0; i < tableModel.getColumnCount(); i++){
            Attribute a = null;

            if(tableModel.getColumnName(i).equals("Actual value") || tableModel.getColumnName(i).equals("Predicted value")){
                a = new Attribute(tableModel.getColumnName(i));
            }else{
                a = new Attribute(tableModel.getColumnName(i), (ArrayList<String>) null);
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
                    values[c] = tableInst.attribute(c).addStringValue((String)tableModel.getValueAt(r, c));
                }
            }
            
            Instance row = new DenseInstance(1.0, values);
            tableInst.add(row);
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
                ChartUtilities.saveChartAsPNG(f, chart, 600, 400);
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
            Page page = pdfDoc.createPage(new Rectangle(600, 400));
            PDFGraphics2D g2 = page.getGraphics2D();
            
            chart.draw(g2, new Rectangle(0, 0, 600, 400));
            pdfDoc.writeToFile(f);
        }
    }
    
    private void existBBDD(){
        Properties p = new Properties();
        
        try {
            p.load(new FileReader("C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DatabaseUtils.props"));
        } catch (IOException ex) {
            m_Log.logMessage(ex.getMessage());
            m_Log.statusMessage("See error log");
        }
        
        String driver = p.getProperty("jdbcDriver");
        
        try {
           Class.forName(driver);
        } catch (ClassNotFoundException ex){
           m_Log.logMessage(ex.getMessage());
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
                String file = "C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DB/DB.sql";
                try {
                    runner.runScript(new BufferedReader(new FileReader(file)));
                } catch (IOException ex) {
                    Logger.getLogger(AttrSelExp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (SQLException ex) {
            m_Log.logMessage(ex.getMessage());
            m_Log.statusMessage("See error log");
        }
    }
    
    private void saveBBDD(){
        if(resultsAttrSelExp != null){
            existBBDD();
            Properties p = new Properties();

            try {
                p.load(new FileReader("C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DatabaseUtils.props"));
            } catch (IOException ex) {
                m_Log.logMessage(ex.getMessage());
                m_Log.statusMessage("See error log");
            }

            String driver = p.getProperty("jdbcDriver");

            try {
               Class.forName(driver);
            } catch (ClassNotFoundException ex){
               m_Log.logMessage(ex.getMessage());
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
                        String file = "C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DB/experiment_group.sql";
                        
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
                        String eval = (String)metricsTableModel.getValueAt(i, 1);
                        String search = (String)metricsTableModel.getValueAt(i, 2);
                        String cls = (String)metricsTableModel.getValueAt(i, 3);
                        int numAttr = (int)metricsTableModel.getValueAt(i, 4);

                        try{
                            st.executeUpdate("INSERT INTO experiment (idexperiment_group, dataset, evaluator, search, classifier) "
                            + "VALUES ('"+id+"','"+dt+"','"+eval+"','"+search+"','"+cls+"')");
                        } catch(Exception ex){
                            ScriptRunner runner = new ScriptRunner(conn, false, false);
                            String file = "C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DB/experiment.sql";
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
                            String file = "C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DB/metrics.sql";
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
                                        st.executeUpdate("INSERT INTO predictions (idexperiment, actual_value, predicted_value, dataset, evaluator, search, classifier, attributes, index_attr_selected) "
                                        + "VALUES ('"+idExp+"','"+valuesPredict.get(z).actual()+"','"+valuesPredict.get(z).predicted()+"','"+dt+"','"+eval+"','"+search+"','"+cls+"','"+data.get(z)+"','"+dataIndexAttrSel.get(z)+"')");
                                    } catch(Exception ex){
                                        ScriptRunner runner = new ScriptRunner(conn, false, false);
                                        String file = "C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DB/predictions.sql";
                                        try {
                                            runner.runScript(new BufferedReader(new FileReader(file)));

                                            st.executeUpdate("INSERT INTO predictions (idexperiment, actual_value, predicted_value, dataset, evaluator, search, classifier, attributes, index_attr_selected) "
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
                                        st.executeUpdate("INSERT INTO predictions (idexperiment, actual_value, predicted_value, dataset, evaluator, search, classifier) "
                                        + "VALUES ('"+idExp+"','"+valuesPredict.get(z).actual()+"','"+valuesPredict.get(z).predicted()+"','"+dt+"','"+eval+"','"+search+"','"+cls+"')");
                                    } catch(Exception ex){
                                        ScriptRunner runner = new ScriptRunner(conn, false, false);
                                        String file = "C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DB/predictions.sql";
                                        try {
                                            runner.runScript(new BufferedReader(new FileReader(file)));

                                            st.executeUpdate("INSERT INTO predictions (idexperiment, actual_value, predicted_value, dataset, evaluator, search, classifier) "
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
        
        try {
            p.load(new FileReader("C:/Users/Usuario/wekafiles/packages/FeatureSelectionStudio/DatabaseUtils.props"));
        } catch (IOException ex) {
            m_Log.logMessage(ex.getMessage());
            m_Log.statusMessage("See error log");
        }
        
        String driver = p.getProperty("jdbcDriver");
        
        try {
           Class.forName(driver);
        } catch (ClassNotFoundException ex){
           m_Log.logMessage(ex.getMessage());
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
                        ResultSet rs3 = st.executeQuery("SELECT actual_value, predicted_value, dataset, evaluator, search,"
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
                            
                            fila[0] = rs3.getDouble("actual_value");
                            fila[1] = rs3.getDouble("predicted_value");
                            
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
        
        int numThreads = 4;
        int numObjects = listIntances.size()*listClassifier.size();
        
        if(!listEvaluators.isEmpty()){
            numObjects += listIntances.size()*listClassifier.size()*listEvaluators.size();
        }
        
        if(!numThreadsTextField.getText().trim().equals("")){
            try{
                numThreads = Integer.parseInt(numThreadsTextField.getText().trim());
            } catch(NumberFormatException ex){
                m_Log.logMessage(ex.getMessage());
            }
        }
        
        executor = Executors.newFixedThreadPool(numThreads);
        Collection<SearchEvaluatorAndClassifier> concurrentExp = new LinkedList<>();
        
        if(!listEvaluators.isEmpty() && !listSearchAlgorithms.isEmpty()){
            for(int i = 0; i < listIntances.size(); i++){
                //listEvaluators and listSearchAlgorithms are the same size
                for(int j = 0; j < listEvaluators.size(); j++){
                    for(int z = 0; z < listClassifier.size(); z++){
                        Instances inst = new Instances(listIntances.get(i));

                        //testMode = 0 holdOutSplit, 1 = crossValidation, 2 = leaveOneOut
                        int testMode = 0;
                        int numFolds = 10;
                        int seed = 1;
                        int classIndex = listIntances.get(i).classIndex();
                        double percent = 70;

                        ASEvaluation evaluator = (ASEvaluation) listEvaluators.get(j);
                        ASSearch search = (ASSearch) listSearchAlgorithms.get(j);

                        try {
                            if (holdOutSplitBtn.isSelected()) {
                                testMode = 0;
                                percent = Double.parseDouble(holdOutSplitTextField.getText());

                                if ((percent <= 0) || (percent >= 100)) {
                                    throw new Exception("Percentage must be between 0 and 100");
                                }
                            }else if (crossValidationBtn.isSelected()) {
                                testMode = 1;
                                numFolds = Integer.parseInt(crossValidationTextField.getText());
                                if (numFolds <= 1) {
                                    throw new Exception("Number of folds must be greater than 1");
                                }
                            }else if (leaveOneOutBtn.isSelected()) {
                                testMode = 2;
                                numFolds = inst.numInstances();
                            }

                            //Classifier
                            Classifier cls = (Classifier) listClassifier.get(z);

                            concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, numFolds, seed, classIndex, 
                                    percent, evaluator, search, cls, progressExp, Math.round(100/numObjects)));

                        } catch (Exception ex) {
                            m_Log.logMessage(ex.getMessage());
                            m_Log.statusMessage("See error log");
                        }
                    }
                }
            }
        }
        
        //no attribute selection
        for(int i = 0; i < listIntances.size(); i++){
            for(int j = 0; j < listClassifier.size(); j++){
                Instances inst = new Instances(listIntances.get(i));

                //testMode = 0 holdOutSplit, 1 = crossValidation, 2 = leaveOneOut
                int testMode = 0;
                int numFolds = 10;
                int seed = 1;
                int classIndex = listIntances.get(i).classIndex();
                double percent = 70;

                try {
                    if (holdOutSplitBtn.isSelected()) {
                        testMode = 0;
                        percent = Double.parseDouble(holdOutSplitTextField.getText());

                        if ((percent <= 0) || (percent >= 100)) {
                            throw new Exception("Percentage must be between 0 and 100");
                        }
                    }else if (crossValidationBtn.isSelected()) {
                        testMode = 1;
                        numFolds = Integer.parseInt(crossValidationTextField.getText());
                        if (numFolds <= 1) {
                            throw new Exception("Number of folds must be greater than 1");
                        }
                    }else if (leaveOneOutBtn.isSelected()) {
                        testMode = 2;
                        numFolds = inst.numInstances();
                    }

                    //Classifier
                    Classifier cls = (Classifier) listClassifier.get(j);

                    concurrentExp.add(new SearchEvaluatorAndClassifier(m_Log, inst, testMode, numFolds, seed, classIndex, 
                            percent, null, null, cls, progressExp, Math.round(100/numObjects)));
                } catch (Exception ex) {
                    m_Log.logMessage(ex.getMessage());
                    m_Log.statusMessage("See error log");
                }
            }
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
        if((resultsAttrSelExp.size() == numObjects) && (progressExp.getValue() < 100)){
            progressExp.setValue(100);
        }else if(resultsAttrSelExp.size() != numObjects){
            progressExp.setValue(0);
        }

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
    private javax.swing.JLabel actionsDatasetsLabel;
    private javax.swing.JLabel actionsFeatureLabel;
    private javax.swing.JButton addClassifierBtn;
    private javax.swing.JButton addFeatureBtn;
    private javax.swing.JButton addFileBtn;
    private javax.swing.JTabbedPane attrSelExpTabs;
    private javax.swing.JLabel attributesLabel;
    private javax.swing.JTextField attributesTextField;
    private javax.swing.JCheckBox aucMetricsCheckBox;
    private javax.swing.JPanel barChartPanel;
    private javax.swing.JLabel classDatasetsLabel;
    private javax.swing.JLabel classificationMetricsLabel;
    private javax.swing.JPanel classifier;
    private javax.swing.JLabel classifierLabel;
    private javax.swing.JList<String> classifierList;
    private javax.swing.JScrollPane classifierListScrollPane;
    private javax.swing.JScrollPane classifierScrollPane;
    private javax.swing.JPanel classifierSelectionPanel;
    private javax.swing.JTable classifierTable;
    private javax.swing.JLabel classifiersPredictionsLabel;
    private javax.swing.JRadioButton crossValidationBtn;
    private javax.swing.JLabel crossValidationLabel;
    private javax.swing.JTextField crossValidationTextField;
    private javax.swing.JPanel datasets;
    private javax.swing.JList<String> datasetsList;
    private javax.swing.JScrollPane datasetsListScrollPane;
    private javax.swing.JLabel datasetsPredictionsLabel;
    private javax.swing.JScrollPane datasetsScrollPane;
    private javax.swing.JTable datasetsTable;
    private javax.swing.JLabel evaluatorLabel;
    private javax.swing.JList<String> evaluatorList;
    private javax.swing.JScrollPane evaluatorListScrollPane;
    private javax.swing.JPanel evaluatorPanel;
    private javax.swing.JLabel evaluatorsPredictionsLabel;
    private javax.swing.JPanel experiment;
    private javax.swing.JCheckBox fMeasureMetricsCheckBox;
    private javax.swing.JScrollPane featureScrollPane;
    private javax.swing.JPanel featureSelection;
    private javax.swing.JTable featuresTable;
    private javax.swing.JPanel graph;
    private javax.swing.JRadioButton holdOutSplitBtn;
    private javax.swing.JLabel holdOutSplitLabel;
    private javax.swing.JTextField holdOutSplitTextField;
    private javax.swing.JCheckBox kappaMetricsCheckBox;
    private javax.swing.JRadioButton leaveOneOutBtn;
    private javax.swing.JButton loadBBDDBtn;
    private javax.swing.JButton loadBtnClassifier;
    private javax.swing.JButton loadBtnDataset;
    private javax.swing.JButton loadBtnFeature;
    private javax.swing.JButton loadDatasetMetricsBtn;
    private javax.swing.JButton loadDatasetPredictionsBtn;
    private javax.swing.JCheckBox maeMetricsCheckBox;
    private javax.swing.JCheckBox mapeMetricsCheckBox;
    private javax.swing.JCheckBox mccMetricsCheckBox;
    private javax.swing.JComboBox<String> metricGraphComboBox;
    private javax.swing.JLabel metricGraphLabel;
    private javax.swing.JPanel metrics;
    private javax.swing.JScrollPane metricsScrollPane;
    private javax.swing.JTable metricsTable;
    private javax.swing.JCheckBox mseMetricsCheckBox;
    private javax.swing.JLabel numThreadsLabel;
    private javax.swing.JTextField numThreadsTextField;
    private javax.swing.JLabel positiveClassDatasetsLabel;
    private javax.swing.JCheckBox precisionMetricsCheckBox;
    private javax.swing.JPanel predictions;
    private javax.swing.JScrollPane predictionsScrollPane;
    private javax.swing.JTable predictionsTable;
    private javax.swing.JProgressBar progressExp;
    private javax.swing.JCheckBox r2MetricsCheckBox;
    private javax.swing.JCheckBox recallMetricsCheckBox;
    private javax.swing.JLabel regressionMetricsLabel;
    private javax.swing.JButton removeBtnClassifier;
    private javax.swing.JButton removeBtnDataset;
    private javax.swing.JButton removeBtnFeature;
    private javax.swing.JPanel results;
    private javax.swing.JCheckBox rmseMetricsCheckBox;
    private javax.swing.JButton runExpBtn;
    private javax.swing.JPanel runPanel;
    private javax.swing.JButton saveARFFMetricsBtn;
    private javax.swing.JButton saveARFFPredictionsBtn;
    private javax.swing.JButton saveBBDDBtn;
    private javax.swing.JButton saveCSVMetricsBtn;
    private javax.swing.JButton saveCSVPredictionsBtn;
    private javax.swing.JButton savePDFGraphBtn;
    private javax.swing.JButton savePNGGraphBtn;
    private javax.swing.JLabel searchLabel;
    private javax.swing.JList<String> searchList;
    private javax.swing.JScrollPane searchListScrollPane;
    private javax.swing.JPanel searchPanel;
    private javax.swing.JLabel searchsPredictionsLabel;
    private javax.swing.JComboBox<String> selectionClassCombo;
    private javax.swing.JComboBox<String> selectionPositiveClassCombo;
    private javax.swing.JButton stopExpBtn;
    private javax.swing.JButton updateGraphBtn;
    private javax.swing.JButton updatePredictionsBtn;
    private javax.swing.JPanel validation;
    private javax.swing.ButtonGroup validationRadioBtn;
    private javax.swing.JComboBox<String> xAxis1ComboBox;
    private javax.swing.JLabel xAxis1Label;
    private javax.swing.JComboBox<String> xAxis2ComboBox;
    private javax.swing.JLabel xAxis2Label;
    // End of variables declaration//GEN-END:variables
}
