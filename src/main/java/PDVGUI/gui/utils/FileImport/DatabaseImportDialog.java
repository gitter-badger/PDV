package PDVGUI.gui.utils.FileImport;

import PDVGUI.fileimport.MzXMLScanImport;
import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.PDVStart;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.filehandling.FileDisplayDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzidentml.MzIdentMLParser;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.MzIdentMLType;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

/**
 * Selecting mzIdentML or pepXML file and spectrum files, then putting into DB 
 * Created by Ken on 2016/11/11.
 */
public class DatabaseImportDialog extends JDialog {

    private JLabel idFilesLabel;
    private JTextField idFilesTxt;
    private JButton startJButton;
    private JComboBox settingsComboBox;
    private JLabel spectrumFilesLabel;
    private JTextField spectrumFilesTxt;
    private JLabel fragmentIonJLable;
    private JComboBox precursorIonUnit;
    private JTextField fragmentIonAccuracyTxt;

    /**
     * PTM factory import form utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Spectrum Object
     */
    private Object spectrumsFileFactory;
    /**
     * Spectrum file type
     */
    private String spectrumFileType;
    /**
     * Main class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Old frame to restart
     */
    private JFrame oldJFrame;
    /**
     * The identification file
     */
    private File idFile;
    /**
     * Spectrum files list
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
    /**
     * LastSelectedFolder for opening easily
     */
    private String lastSelectedFolder;
    /**
     * Welcome dialog parent
     */
    private PDVStart pdvStart;
    /**
     * MS2 ion tolerance
     */
    private Double fragmentIonMZTolerance = 0.5;
    /**
     * MS2 ion tolerance type (ppm or dal)
     */
    private PDVMainClass.MassAccuracyType fragmentAccuracyType;
    /**
     * Object to saving identification results
     */
    private MzIdentMLType mzIdentMLType = null;
    /**
     * Check if all identification imported done or not 
     */
    private int mzIdentMLCheck = 0;

    /**
     * Creates a new open dialog.
     * @param pdvStart the welcome dialog parent frame
     * @param pdvMainClass a reference to the main frame
     * @param modal boolean indicating whether the dialog is modal
     */
    public DatabaseImportDialog(PDVStart pdvStart, PDVMainClass pdvMainClass, boolean modal) {
        super(pdvStart, modal);
        this.pdvMainClass = pdvMainClass;
        this.pdvStart = pdvStart;

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvStart);
        setVisible(true);
    }

    /**
     * Creates a new open dialog.
     * @param pdvMainClass a reference to the main frame
     * @param modal boolean indicating whether the dialog is modal
     */
    public DatabaseImportDialog(PDVMainClass pdvMainClass, boolean modal, JFrame oldJFrame) {
        super(pdvMainClass, modal);
        this.pdvMainClass = pdvMainClass;
        this.pdvStart = null;
        this.oldJFrame = oldJFrame;

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvMainClass);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        initComponents();
        this.precursorIonUnit.setEnabled(true);
        this.precursorIonUnit.setRenderer(new AlignedListCellRenderer(0));
        fragmentIonAccuracyTxt.setText(String.valueOf(0.5));

        settingsComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        idFilesTxt.setText( "No selected");
        spectrumFilesTxt.setText("No selected");

        validateInput();
        GuiUtilities.installEscapeCloseOperation(this);
    }

    /**
     * Init all GUI components
     */
    private void initComponents() {
        fragmentIonJLable = new JLabel();
        precursorIonUnit = new JComboBox();
        spectrumFilesLabel = new JLabel();
        spectrumFilesTxt = new JTextField();
        fragmentIonAccuracyTxt = new JTextField();
        startJButton = new JButton();
        settingsComboBox = new JComboBox();
        idFilesLabel = new JLabel();
        idFilesTxt = new JTextField();
        JPanel mainJPanel = new JPanel();
        JPanel annotationSettingJPanel = new JPanel();
        JPanel inputFilesPanel = new JPanel();
        JButton browseIdJButton = new JButton();
        JButton browseSpectraJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - Database Result Display");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainJPanel.setBackground(new Color(250, 250, 250));

        startJButton.setBackground(new Color(255, 0, 11));
        startJButton.setFont(new Font("Lucida", Font.BOLD, 13));
        startJButton.setText("Start");
        startJButton.setEnabled(false);
        startJButton.setOpaque(false);
        startJButton.addActionListener(this::startJButtonActionPerformed);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Project Settings" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        annotationSettingJPanel.setBorder(titledBorder);
        annotationSettingJPanel.setOpaque(false);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        precursorIonUnit.setOpaque(false);
        precursorIonUnit.setBackground(Color.WHITE);
        fragmentIonJLable.setText("Fragment m/z Tolerance");
        fragmentIonJLable.setFont(new Font("Console", Font.PLAIN, 12));
        fragmentIonAccuracyTxt.setHorizontalAlignment(0);
        fragmentIonAccuracyTxt.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                fragmentIonAccuracyTxtKeyReleased(evt);
            }
        });

        GroupLayout annotationSettingJPanelLayout = new GroupLayout(annotationSettingJPanel);
        annotationSettingJPanel.setLayout(annotationSettingJPanelLayout);
        annotationSettingJPanelLayout.setHorizontalGroup(
            annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                                        .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(fragmentIonJLable,  GroupLayout.PREFERRED_SIZE, 220, Short.MAX_VALUE))))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                                        .addComponent(fragmentIonAccuracyTxt, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(precursorIonUnit, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
        );
        annotationSettingJPanelLayout.setVerticalGroup(
            annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                .addContainerGap()
                    .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(fragmentIonJLable, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addComponent(fragmentIonAccuracyTxt)
                            .addComponent(precursorIonUnit))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        titledBorder = BorderFactory.createTitledBorder("Input Files" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        inputFilesPanel.setBorder(titledBorder);
        inputFilesPanel.setOpaque(false);

        idFilesLabel.setForeground(new Color(255, 0, 0));
        idFilesLabel.setFont(new Font("Console", Font.PLAIN, 12));
        idFilesLabel.setText("Results File*");

        idFilesTxt.setHorizontalAlignment(JTextField.CENTER);

        browseIdJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseIdJButton.setBorder(null);
        browseIdJButton.setBorderPainted(false);
        browseIdJButton.setContentAreaFilled(false);
        browseIdJButton.addActionListener(this::browseIdJButtonActionPerformed);

        spectrumFilesLabel.setForeground(new Color(255, 0, 0));
        spectrumFilesLabel.setFont(new Font("Console", Font.PLAIN, 12));
        spectrumFilesLabel.setText("Spectrum File(s)*");

        spectrumFilesTxt.setEditable(false);
        spectrumFilesTxt.setHorizontalAlignment(JTextField.CENTER);
        spectrumFilesTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                spectrumFilesTxtMouseClicked(evt);
            }
        });

        browseSpectraJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseSpectraJButton.setBorder(null);
        browseSpectraJButton.setBorderPainted(false);
        browseSpectraJButton.setContentAreaFilled(false);
        browseSpectraJButton.addActionListener(this::browseSpectraJButtonActionPerformed);

        GroupLayout inputFilesPanelLayout = new GroupLayout(inputFilesPanel);
        inputFilesPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
            inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(idFilesLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(idFilesTxt,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseIdJButton))
                    .addGroup(inputFilesPanelLayout.createSequentialGroup()
                        .addComponent(spectrumFilesLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(spectrumFilesTxt,GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(browseSpectraJButton)))
                .addContainerGap())
        );

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseIdJButton});

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseSpectraJButton});

        inputFilesPanelLayout.setVerticalGroup(
            inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(idFilesTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(idFilesLabel)
                    .addComponent(browseIdJButton))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(spectrumFilesLabel)
                    .addComponent(browseSpectraJButton)
                    .addComponent(spectrumFilesTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
            mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(mainJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(mainJPanelLayout.createSequentialGroup()
                        .addGap(150, 220, 500)
                        .addComponent(startJButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15))
                    .addComponent(inputFilesPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(annotationSettingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
            mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(mainJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(inputFilesPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(annotationSettingJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(startJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(mainJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }

    /**
     * Import all files and open newMain frame
     * @param evt mouse click event
     */
    private void startJButtonActionPerformed(ActionEvent evt) {

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }

        if(oldJFrame != null){
            oldJFrame.setVisible(false);
        }

        this.setVisible(false);
        pdvMainClass.setVisible(true);

        ProgressDialogX progressDialog = new ProgressDialogX(pdvMainClass,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Data. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException e) {
            }
        }, "ProgressDialog").start();

        new Thread("SetUp"){
            @Override
            public void run(){

                Integer threads = Runtime.getRuntime().availableProcessors();

                ArrayList<String> modification =  ptmFactory.getPTMs();
                PtmSettings ptmSettings = new PtmSettings();

                for(String fixedModification:modification){
                    ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
                }

                for(String variableModification:modification){
                    ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
                }

                SearchParameters searchParameters = new SearchParameters();
                searchParameters.setPtmSettings(ptmSettings);
                if(precursorIonUnit.getSelectedIndex() == 0){
                    searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.DA);
                }else {
                    searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.PPM);
                }
                searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);
                
                AnnotationSettings annotationPreferences = new AnnotationSettings();
                annotationPreferences.setPreferencesFromSearchParameters(searchParameters);
                annotationPreferences.setFragmentIonAccuracy(fragmentIonMZTolerance);
                pdvMainClass.setAnnotationSettings(annotationPreferences);
                pdvMainClass.setSearchParameters(searchParameters);

                if(precursorIonUnit.getSelectedIndex() == 0){
                    fragmentAccuracyType = PDVMainClass.MassAccuracyType.DA;
                }else {
                    fragmentAccuracyType = PDVMainClass.MassAccuracyType.PPM;
                }
                pdvMainClass.setFragmentAccuracyType(fragmentAccuracyType);

                if (idFile != null) {

                    if(spectrumFileType.equals("mgf")){
                        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
                        try {
                            for(File spectrumFile: spectrumFiles){
                                spectrumFactory.addSpectra(spectrumFile);
                            }
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(DatabaseImportDialog.this, "Failed to pares Spectrum file. Please check your spectrum file!", "File Error", JOptionPane.WARNING_MESSAGE);
                            progressDialog.setRunFinished();
                            e.printStackTrace();
                            System.exit(1);
                        }

                        spectrumsFileFactory = spectrumFactory;

                    }else if(spectrumFileType.equals("mzml")){

                        MZMLFile mzmlFile = new MZMLFile(spectrumFiles.get(0).getAbsolutePath());

                        ScanCollectionDefault scans = new ScanCollectionDefault();

                        scans.setDefaultStorageStrategy(StorageStrategy.SOFT);

                        scans.isAutoloadSpectra(true);

                        scans.setDataSource(mzmlFile);

                        mzmlFile.setNumThreadsForParsing(threads);

                        try {
                            scans.loadData(LCMSDataSubset.MS2_WITH_SPECTRA);
                        } catch (FileParsingException e) {
                            JOptionPane.showMessageDialog(DatabaseImportDialog.this, "Failed to pares Spectrum file. Please check your spectrum file!", "File Error", JOptionPane.WARNING_MESSAGE);
                            progressDialog.setRunFinished();
                            e.printStackTrace();
                            System.exit(1);
                        }

                        spectrumsFileFactory = scans;
                    }else if(spectrumFileType.equals("mzxml")){

                        MzXMLScanImport mzXMLScanImport = new MzXMLScanImport(spectrumFiles.get(0).getAbsolutePath());

                        spectrumsFileFactory = mzXMLScanImport.getScans();
                    }

                    for (int i = 0;i<300;i++){
                        if (mzIdentMLCheck ==1){

                            progressDialog.setRunFinished();

                            importIdentificationFiles();

                            break;
                        }else{
                            try {Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }

            }
        }.start();

        this.dispose();
    }

    /**
     * fragmentIonAccuracyTxtKeyReleased
     * @param evt key event
     */
    private void fragmentIonAccuracyTxtKeyReleased(KeyEvent evt) {
        if(!Objects.equals(fragmentIonAccuracyTxt.getText(), "") && fragmentIonAccuracyTxt.getText() != null){
            fragmentIonMZTolerance = Double.valueOf(fragmentIonAccuracyTxt.getText());
        } else {
            fragmentIonMZTolerance = null;
        }
        validateInput();
    }

    /**
     * Select all spectrum files
     * @param evt mouse click event
     */
    private void browseSpectraJButtonActionPerformed(ActionEvent evt) {

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Spectrum File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        ||myFile.getName().toLowerCase().endsWith(".mzml")
                        ||myFile.getName().toLowerCase().endsWith(".mzxml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot Generic Format (.mgf)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnValue = fileChooser.showDialog(this, "Add");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    File[] selectedFiles = newFile.listFiles();
                    for (File file : selectedFiles) {
                        if (file.getName().toLowerCase().endsWith(".mgf")) {
                            spectrumFiles.add(file);
                        }
                    }
                } else {
                    if(newFile.getName().toLowerCase().endsWith(".mgf")){
                        spectrumFileType = "mgf";
                    }else if(newFile.getName().toLowerCase().endsWith(".mzml")){
                        spectrumFileType = "mzml";
                    }else {
                        spectrumFileType = "mzxml";
                    }
                    spectrumFiles.add(newFile);
                }
            }

            spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
            validateInput();
        }
    }

    /**
     * Select identification file
     * @param evt mouse click event
     */
    private void browseIdJButtonActionPerformed(ActionEvent evt) {

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Identification File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter mzidFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mzid")
                        ||myFile.getName().toLowerCase().endsWith(".pepxml")
                        ||myFile.getName().toLowerCase().endsWith(".txt")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "mzIdentML (.mzid), PepXML (.pepxml), New Id File (.txt)";
            }
        };

        fileChooser.setFileFilter(mzidFilter);

        int returnValue = fileChooser.showDialog(this, "Add");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            idFile = fileChooser.getSelectedFile();

            lastSelectedFolder = idFile.getParent();

            idFilesTxt.setText(idFile.getName() + " selected");

            if(idFile.getName().toLowerCase().endsWith(".mzid")) {
                Thread importIDThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            mzIdentMLType = MzIdentMLParser.parse(Paths.get(idFile.getAbsolutePath()));//The main time consuming!!!
                            mzIdentMLCheck = 1;
                        } catch (FileParsingException |Error e ) {
                            if(e.getClass().getName().contains("Error")){
                                JOptionPane.showMessageDialog(pdvMainClass, "Please increase JVM memory! ", "Memory Error", JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(DatabaseImportDialog.this, "Failed to pares Identification file", "File Error", JOptionPane.WARNING_MESSAGE);
                                e.printStackTrace();
                            }
                        }
                    }
                }, "importThread");

                importIDThread.start();
            }else {
                mzIdentMLCheck = 1;
            }

            validateInput();
        }
    }

    /**
     * Display selected spectrum files and choose to delete
     * @param evt mouse click event
     */
    private void spectrumFilesTxtMouseClicked(MouseEvent evt) {
        if (!spectrumFiles.isEmpty()) {
            FileDisplayDialog fileDisplayDialog = new FileDisplayDialog(this, spectrumFiles, true);
            if (!fileDisplayDialog.canceled()) {
                spectrumFiles = fileDisplayDialog.getSelectedFiles();
                spectrumFilesTxt.setText(spectrumFiles.size() + " file(s) selected");
                validateInput();
            }
        }
    }

    /**
     * Closes the dialog
     * @param evt window click event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Validates input information and enable start button
     */
    private void validateInput() {

        boolean allValid = true;

        if (idFile != null) {
            idFilesLabel.setForeground(Color.BLACK);
            idFilesLabel.setToolTipText(null);
        } else {
            idFilesLabel.setForeground(Color.RED);
            idFilesLabel.setToolTipText("Please select at least one identification file");
            idFilesTxt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (spectrumFiles.size() != 0) {
            spectrumFilesLabel.setForeground(Color.BLACK);
            spectrumFilesLabel.setToolTipText(null);
            spectrumFilesTxt.setToolTipText("Click to see the selected files");
            spectrumFilesTxt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else {
            spectrumFilesLabel.setForeground(Color.RED);
            spectrumFilesLabel.setToolTipText("Please select the spectrum file(s) for the identification file");
            spectrumFilesTxt.setToolTipText("Please select the spectrum file(s) for the identification file");
            spectrumFilesTxt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (fragmentIonMZTolerance != null) {
            fragmentIonJLable.setForeground(Color.BLACK);
            fragmentIonJLable.setToolTipText(null);
        } else {
            fragmentIonJLable.setForeground(Color.RED);
            fragmentIonJLable.setToolTipText("Please input MS2 ion tolerance!");
            allValid = false;
        }

        if (allValid){
            startJButton.setBackground(new Color(39, 239, 57));
        }
        startJButton.setEnabled(allValid);
    }

    /**
     * Import all files
     */
    private void importIdentificationFiles() {

        try {
            if(idFile.getName().toLowerCase().endsWith(".mzid")) {

                pdvMainClass.importMzID(spectrumsFileFactory, idFile, mzIdentMLType, spectrumFileType);
                idFile = null;

            }else if(idFile.getName().toLowerCase().endsWith("xml")){
                pdvMainClass.importFilePep(spectrumFiles.get(0), spectrumsFileFactory, idFile, spectrumFileType);
                idFile = null;
            } else if(idFile.getName().toLowerCase().endsWith(".txt")){
                pdvMainClass.importTextResults(spectrumFiles.get(0), spectrumsFileFactory, idFile, spectrumFileType);
                idFile = null;
            } else {
                JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                        "No support ID file format, please check your file."),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setIconImage(Image image) {
        super.setIconImage(image);
        if (pdvStart != null) {
            pdvStart.setIconImage(image);
        }
    }

}
