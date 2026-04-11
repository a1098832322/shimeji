/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import com.nilo.plaf.nimrod.NimRODFontDialog;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import com.nilo.plaf.nimrod.NimRODTheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 */
public class SettingsWindow extends javax.swing.JDialog
{
    private final Path configPath = Paths.get( ".", "conf", "settings.properties" );	// Config file name
    private final Path themePath = Paths.get( ".", "conf", "theme.properties" );
    private NimRODTheme theme;
    private NimRODTheme oldTheme;
    private NimRODLookAndFeel lookAndFeel;
    private final ArrayList<String> listData = new ArrayList<String>( );
    private final ArrayList<String> blacklistData = new ArrayList<String>( );
    private Boolean alwaysShowShimejiChooser = false;
    private Boolean alwaysShowInformationScreen = false;
    private String filter = "nearest";
    private double scaling = 1.0;
    private double opacity = 1.0;
    private Boolean windowedMode = false;
    private Dimension windowSize = new Dimension( 600, 500 );
    private Color backgroundColour = new Color( 0, 255, 0 );
    private String backgroundMode = "centre";
    private String backgroundImage = null;
    private final String[ ] backgroundModes = { "centre", "fill", "fit", "stretch" };
    private Color primaryColour1;
    private Color primaryColour2;
    private Color primaryColour3;
    private Color secondaryColour1;
    private Color secondaryColour2;
    private Color secondaryColour3;
    private Color blackColour;
    private Color whiteColour;
    private Font font;
    private double menuOpacity = 1.0;
    private Boolean colourWasChanged = false;
    
    private Boolean suppressTextChanged = true;
    private Boolean imageReloadRequired = false;
    private Boolean interactiveWindowReloadRequired = false;
    private Boolean environmentReloadRequired = false;
    
    /**
     * Creates new form SettingsWindow
     */    
    public SettingsWindow( java.awt.Frame parent, boolean modal )
    {
        super( parent, modal );
        initComponents( );
    }
    
    public void init( )
    {
        // initialise controls
        setLocationRelativeTo( null );
        grpFilter.add( radFilterNearest );
        grpFilter.add( radFilterBicubic );
        grpFilter.add( radFilterHqx );
        java.util.Hashtable<Integer,JLabel> labelTable = new java.util.Hashtable<Integer,JLabel>( );
        for( int index = 0; index < 9; index++ )
            labelTable.put( index * 10, new JLabel( index + "x" ) );
        sldScaling.setLabelTable( labelTable );
        sldScaling.setPaintLabels( true );
        sldScaling.setSnapToTicks( true );
        
        // load existing settings
        Properties properties = Main.getInstance( ).getProperties( );
        alwaysShowShimejiChooser = Boolean.parseBoolean( properties.getProperty( "AlwaysShowShimejiChooser", "false" ) );
        alwaysShowInformationScreen = Boolean.parseBoolean( properties.getProperty( "AlwaysShowInformationScreen", "false" ) );
        String filterText = Main.getInstance( ).getProperties( ).getProperty( "Filter", "false" );
        filter = "nearest";
        if( filterText.equalsIgnoreCase( "true" ) || filterText.equalsIgnoreCase( "hqx" ) )
            filter = "hqx";
        else if( filterText.equalsIgnoreCase( "bicubic" ) )
            filter = "bicubic";
        opacity = Double.parseDouble( properties.getProperty( "Opacity", "1.0" ) );
        scaling = Double.parseDouble( properties.getProperty( "Scaling", "1.0" ) );
        windowedMode = properties.getProperty( "Environment", "generic" ).equals( "virtual" );
        String[ ] windowArray = properties.getProperty( "WindowSize", "600x500" ).split( "x" );
        windowSize = new Dimension( Integer.parseInt( windowArray[ 0 ] ), Integer.parseInt( windowArray[ 1 ] ) );
        backgroundColour = Color.decode( properties.getProperty( "Background", "#00FF00" ) );
        backgroundImage = properties.getProperty( "BackgroundImage", "" );
        backgroundMode = properties.getProperty( "BackgroundMode", "centre" );
        float menuScaling = Float.parseFloat( properties.getProperty( "MenuDPI", "96" ) ) / 96;
        chkAlwaysShowShimejiChooser.setSelected( alwaysShowShimejiChooser );
        chkAlwaysShowInformationScreen.setSelected( alwaysShowInformationScreen );
        if( filter.equals( "bicubic" ) )
            radFilterBicubic.setSelected( true );
        else if( filter.equals( "hqx" ) )
            radFilterHqx.setSelected( true );
        else
            radFilterNearest.setSelected( true );
        sldOpacity.setValue( (int)( opacity * 100 ) );
        sldScaling.setValue( (int)( scaling * 10 ) );
        
        for( String item : properties.getProperty( "InteractiveWindows", "" ).split( "/" ) )
            if( !item.isEmpty( ) )
                listData.add( item );
        lstInteractiveWindows.setListData( listData.toArray( ) );
        for( String item : properties.getProperty( "InteractiveWindowsBlacklist", "" ).split( "/" ) )
            if( !item.isEmpty( ) )
                blacklistData.add( item );
        lstInteractiveWindowsBlacklist.setListData( blacklistData.toArray( ) );
        
        Properties themeProperties = new Properties( );
        FileInputStream input;
        try
        {
            input = new FileInputStream( themePath.toFile( ) );
            themeProperties.load( input );
        }
        catch( FileNotFoundException ex )
        {
        }
        catch( IOException ex )
        {
        }
        primaryColour1 = Color.decode( themeProperties.getProperty( "nimrodlf.p1", "#1EA6EB" ) );
        primaryColour2 = Color.decode( themeProperties.getProperty( "nimrodlf.p2", "#28B0F5" ) );
        primaryColour3 = Color.decode( themeProperties.getProperty( "nimrodlf.p3", "#32BAFF" ) );
        secondaryColour1 = Color.decode( themeProperties.getProperty( "nimrodlf.s1", "#BCBCBE" ) );
        secondaryColour2 = Color.decode( themeProperties.getProperty( "nimrodlf.s2", "#C6C6C8" ) );
        secondaryColour3 = Color.decode( themeProperties.getProperty( "nimrodlf.s3", "#D0D0D2" ) );
        blackColour = Color.decode( themeProperties.getProperty( "nimrodlf.b", "#000000" ) );
        whiteColour = Color.decode( themeProperties.getProperty( "nimrodlf.w", "#FFFFFF" ) );
        menuOpacity = Integer.parseInt( properties.getProperty( "nimrodlf.menuOpacity", "255" ) ) / 255;
        font = Font.decode( themeProperties.getProperty( "nimrodlf.font", "SansSerif-PLAIN-12" ) );
        font = font.deriveFont( font.getSize( ) * menuScaling );
        pnlPrimaryColour1Preview.setBackground( primaryColour1 );
        txtPrimaryColour1.setText( String.format( "#%02X%02X%02X", primaryColour1.getRed( ), primaryColour1.getGreen( ), primaryColour1.getBlue( ) ) );
        pnlPrimaryColour2Preview.setBackground( primaryColour2 );
        txtPrimaryColour2.setText( String.format( "#%02X%02X%02X", primaryColour2.getRed( ), primaryColour2.getGreen( ), primaryColour2.getBlue( ) ) );
        pnlSecondaryColour1Preview.setBackground( secondaryColour1 );
        txtSecondaryColour1.setText( String.format( "#%02X%02X%02X", secondaryColour1.getRed( ), secondaryColour1.getGreen( ), secondaryColour1.getBlue( ) ) );
        pnlSecondaryColour2Preview.setBackground( secondaryColour2 );
        txtSecondaryColour2.setText( String.format( "#%02X%02X%02X", secondaryColour2.getRed( ), secondaryColour2.getGreen( ), secondaryColour2.getBlue( ) ) );
        pnlSecondaryColour3Preview.setBackground( secondaryColour3 );
        txtSecondaryColour3.setText( String.format( "#%02X%02X%02X", secondaryColour3.getRed( ), secondaryColour3.getGreen( ), secondaryColour3.getBlue( ) ) );
        pnlBlackColourPreview.setBackground( blackColour );
        txtBlackColour.setText( String.format( "#%02X%02X%02X", blackColour.getRed( ), blackColour.getGreen( ), blackColour.getBlue( ) ) );
        pnlWhiteColourPreview.setBackground( whiteColour );
        txtWhiteColour.setText( String.format( "#%02X%02X%02X", whiteColour.getRed( ), whiteColour.getGreen( ), whiteColour.getBlue( ) ) );
        theme = new NimRODTheme( );
        theme.setPrimary1( primaryColour1 );
        theme.setPrimary2( primaryColour2 );
        theme.setPrimary3( primaryColour3 );
        theme.setSecondary1( secondaryColour1 );
        theme.setSecondary2( secondaryColour2 );
        theme.setSecondary3( secondaryColour3 );
        theme.setBlack( blackColour );
        theme.setWhite( whiteColour );
        sldMenuOpacity.setValue( (int)( menuOpacity * 100 ) );
        theme.setFont( font );
        oldTheme = new NimRODTheme( );
        oldTheme.setPrimary1( primaryColour1 );
        oldTheme.setPrimary2( primaryColour2 );
        oldTheme.setPrimary3( primaryColour3 );
        oldTheme.setSecondary1( secondaryColour1 );
        oldTheme.setSecondary2( secondaryColour2 );
        oldTheme.setSecondary3( secondaryColour3 );
        oldTheme.setBlack( blackColour );
        oldTheme.setWhite( whiteColour );
        oldTheme.setMenuOpacity( (int)( menuOpacity * 255 ) );
        oldTheme.setFont( font );
        lookAndFeel = (NimRODLookAndFeel)UIManager.getLookAndFeel( );
        
        chkWindowModeEnabled.setSelected( windowedMode );
        spnWindowWidth.setBackground( txtBackgroundColour.getBackground( ) );
        spnWindowHeight.setBackground( txtBackgroundColour.getBackground( ) );
        spnWindowWidth.setEnabled( windowedMode );
        spnWindowHeight.setEnabled( windowedMode );
        spnWindowWidth.setValue( windowSize.width );
        spnWindowHeight.setValue( windowSize.height );
        txtBackgroundColour.setText( String.format( "#%02X%02X%02X", backgroundColour.getRed( ), backgroundColour.getGreen( ), backgroundColour.getBlue( ) ) );
        btnBackgroundColourChange.setEnabled( windowedMode );
        btnBackgroundImageChange.setEnabled( windowedMode );
        pnlBackgroundPreview.setBackground( backgroundColour );
        if( backgroundImage != null )
        {
            try
            {
                Dimension size = pnlBackgroundImage.getPreferredSize( );
                refreshBackgroundImage( );
                pnlBackgroundImage.setPreferredSize( size );
            }
            catch( Exception e )
            {
                backgroundImage = null;
                lblBackgroundImage.setIcon( null );
            }
        }
        cmbBackgroundImageMode.setEnabled( windowedMode && backgroundImage != null );
        btnBackgroundImageRemove.setEnabled( windowedMode && backgroundImage != null );
        
        // localisation
        Properties language = Main.getInstance( ).getLanguageBundle( );
        setTitle( language.getProperty( "Settings" ) );
        pnlTabs.setTitleAt( 0, language.getProperty( "General" ) );
        pnlTabs.setTitleAt( 1, language.getProperty( "InteractiveWindows" ) );
        pnlTabs.setTitleAt( 2, language.getProperty( "Theme" ) );
        pnlTabs.setTitleAt( 3, language.getProperty( "WindowMode" ) );
        pnlTabs.setTitleAt( 4, language.getProperty( "About" ) );
        lblShimejiEE.setText( language.getProperty( "ShimejiEE" ) );
        lblDevelopedBy.setText( language.getProperty( "DevelopedBy" ) );
        chkAlwaysShowShimejiChooser.setText( language.getProperty( "AlwaysShowShimejiChooser" ) );
        chkAlwaysShowInformationScreen.setText( language.getProperty( "AlwaysShowInformationScreen" ) );
        lblOpacity.setText( language.getProperty( "Opacity" ) );
        lblScaling.setText( language.getProperty( "Scaling" ) );
        lblFilter.setText( language.getProperty( "FilterOptions" ) );
        radFilterNearest.setText( language.getProperty( "NearestNeighbour" ) );
        radFilterHqx.setText( language.getProperty( "Filter" ) );
        radFilterBicubic.setText( language.getProperty( "BicubicFilter" ) );
        pnlInteractiveTabs.setTitleAt( 0, language.getProperty( "Whitelist" ) );
        pnlInteractiveTabs.setTitleAt( 1, language.getProperty( "Blacklist" ) );
        btnAddInteractiveWindow.setText( language.getProperty( "Add" ) );
        btnRemoveInteractiveWindow.setText( language.getProperty( "Remove" ) );
        lblPrimaryColour1.setText( language.getProperty( "PrimaryColour1" ) );
        lblPrimaryColour2.setText( language.getProperty( "PrimaryColour2" ) );
        lblSecondaryColour1.setText( language.getProperty( "SecondaryColour1" ) );
        lblSecondaryColour2.setText( language.getProperty( "SecondaryColour2" ) );
        lblSecondaryColour3.setText( language.getProperty( "SecondaryColour3" ) );
        lblBlackColour.setText( language.getProperty( "BlackColour" ) );
        lblWhiteColour.setText( language.getProperty( "WhiteColour" ) );
        lblMenuOpacity.setText( language.getProperty( "MenuOpacity" ) );
        btnChangeFont.setText( language.getProperty( "ChangeFont" ) );
        btnReset.setText( language.getProperty( "Reset" ) );
        chkWindowModeEnabled.setText( language.getProperty( "WindowedModeEnabled" ) );
        lblDimensions.setText( language.getProperty( "Dimensions" ) );
        lblBackground.setText( language.getProperty( "Background" ) );
        btnBackgroundColourChange.setText( language.getProperty( "Change" ) );
        btnBackgroundImageChange.setText( language.getProperty( "Change" ) );
        cmbBackgroundImageMode.addItem( language.getProperty( "BackgroundModeCentre" ) );
        cmbBackgroundImageMode.addItem( language.getProperty( "BackgroundModeFill" ) );
        cmbBackgroundImageMode.addItem( language.getProperty( "BackgroundModeFit" ) );
        cmbBackgroundImageMode.addItem( language.getProperty( "BackgroundModeStretch" ) );
        btnBackgroundImageRemove.setText( language.getProperty( "Remove" ) );
        lblShimejiEE.setText( language.getProperty( "ShimejiEE" ) );
        lblDevelopedBy.setText( language.getProperty( "DevelopedBy" ) );
        btnWebsite.setText( language.getProperty( "Website" ) );
        btnDone.setText( language.getProperty( "Done" ) );
        btnCancel.setText( language.getProperty( "Cancel" ) );
        
        // come back around to this one now that the dropdown is populated
        for( int index = 0; index < backgroundModes.length; index++ )
        {
            if( backgroundMode.equals( backgroundModes[ index ] ) )
            {
                cmbBackgroundImageMode.setSelectedIndex( index );
                break;
            }
        }
    }
    
    public boolean display( )
    {
        float menuScaling = Float.parseFloat( Main.getInstance( ).getProperties( ).getProperty( "MenuDPI", "96" ) ) / 96;
        
        // scale controls to fit
        getContentPane( ).setPreferredSize( new Dimension( (int)( 600 * menuScaling ), (int)( 497 * menuScaling ) ) );
        sldOpacity.setPreferredSize( new Dimension( (int)( sldOpacity.getPreferredSize( ).width * menuScaling ), (int)( sldOpacity.getPreferredSize( ).height * menuScaling ) ) );
        sldScaling.setPreferredSize( new Dimension( (int)( sldScaling.getPreferredSize( ).width * menuScaling ), (int)( sldScaling.getPreferredSize( ).height * menuScaling ) ) );
        btnAddInteractiveWindow.setPreferredSize( new Dimension( (int)( btnAddInteractiveWindow.getPreferredSize( ).width * menuScaling ), (int)( btnAddInteractiveWindow.getPreferredSize( ).height * menuScaling ) ) );
        btnRemoveInteractiveWindow.setPreferredSize( new Dimension( (int)( btnRemoveInteractiveWindow.getPreferredSize( ).width * menuScaling ), (int)( btnRemoveInteractiveWindow.getPreferredSize( ).height * menuScaling ) ) );
        pnlInteractiveButtons.setPreferredSize( new Dimension( pnlInteractiveButtons.getPreferredSize( ).width, btnAddInteractiveWindow.getPreferredSize( ).height + 6 ) );
        txtPrimaryColour1.setPreferredSize( new Dimension( (int)( txtPrimaryColour1.getPreferredSize( ).width * menuScaling ), (int)( txtPrimaryColour1.getPreferredSize( ).height * menuScaling ) ) );
        txtPrimaryColour2.setPreferredSize( new Dimension( (int)( txtPrimaryColour2.getPreferredSize( ).width * menuScaling ), (int)( txtPrimaryColour2.getPreferredSize( ).height * menuScaling ) ) );
        txtSecondaryColour1.setPreferredSize( new Dimension( (int)( txtSecondaryColour1.getPreferredSize( ).width * menuScaling ), (int)( txtSecondaryColour1.getPreferredSize( ).height * menuScaling ) ) );
        txtSecondaryColour2.setPreferredSize( new Dimension( (int)( txtSecondaryColour2.getPreferredSize( ).width * menuScaling ), (int)( txtSecondaryColour2.getPreferredSize( ).height * menuScaling ) ) );
        txtSecondaryColour3.setPreferredSize( new Dimension( (int)( txtSecondaryColour3.getPreferredSize( ).width * menuScaling ), (int)( txtSecondaryColour3.getPreferredSize( ).height * menuScaling ) ) );
        txtBlackColour.setPreferredSize( new Dimension( (int)( txtBlackColour.getPreferredSize( ).width * menuScaling ), (int)( txtBlackColour.getPreferredSize( ).height * menuScaling ) ) );
        txtWhiteColour.setPreferredSize( new Dimension( (int)( txtWhiteColour.getPreferredSize( ).width * menuScaling ), (int)( txtWhiteColour.getPreferredSize( ).height * menuScaling ) ) );
        pnlPrimaryColour1PreviewContainer.setPreferredSize( new Dimension( (int)( pnlPrimaryColour1PreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlPrimaryColour1PreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlPrimaryColour1Preview.setPreferredSize( new Dimension( (int)( pnlPrimaryColour1Preview.getPreferredSize( ).width * menuScaling ), (int)( pnlPrimaryColour1Preview.getPreferredSize( ).height * menuScaling ) ) );
        pnlPrimaryColour2PreviewContainer.setPreferredSize( new Dimension( (int)( pnlPrimaryColour2PreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlPrimaryColour2PreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlPrimaryColour2Preview.setPreferredSize( new Dimension( (int)( pnlPrimaryColour2Preview.getPreferredSize( ).width * menuScaling ), (int)( pnlPrimaryColour2Preview.getPreferredSize( ).height * menuScaling ) ) );
        pnlSecondaryColour1PreviewContainer.setPreferredSize( new Dimension( (int)( pnlSecondaryColour1PreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlSecondaryColour1PreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlSecondaryColour1Preview.setPreferredSize( new Dimension( (int)( pnlSecondaryColour1Preview.getPreferredSize( ).width * menuScaling ), (int)( pnlSecondaryColour1Preview.getPreferredSize( ).height * menuScaling ) ) );
        pnlSecondaryColour2PreviewContainer.setPreferredSize( new Dimension( (int)( pnlSecondaryColour2PreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlSecondaryColour2PreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlSecondaryColour2Preview.setPreferredSize( new Dimension( (int)( pnlSecondaryColour2Preview.getPreferredSize( ).width * menuScaling ), (int)( pnlSecondaryColour2Preview.getPreferredSize( ).height * menuScaling ) ) );
        pnlSecondaryColour3PreviewContainer.setPreferredSize( new Dimension( (int)( pnlSecondaryColour3PreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlSecondaryColour3PreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlSecondaryColour3Preview.setPreferredSize( new Dimension( (int)( pnlSecondaryColour3Preview.getPreferredSize( ).width * menuScaling ), (int)( pnlSecondaryColour3Preview.getPreferredSize( ).height * menuScaling ) ) );
        pnlBlackColourPreviewContainer.setPreferredSize( new Dimension( (int)( pnlBlackColourPreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlBlackColourPreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlBlackColourPreview.setPreferredSize( new Dimension( (int)( pnlBlackColourPreview.getPreferredSize( ).width * menuScaling ), (int)( pnlBlackColourPreview.getPreferredSize( ).height * menuScaling ) ) );
        pnlWhiteColourPreviewContainer.setPreferredSize( new Dimension( (int)( pnlWhiteColourPreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlWhiteColourPreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlWhiteColourPreview.setPreferredSize( new Dimension( (int)( pnlWhiteColourPreview.getPreferredSize( ).width * menuScaling ), (int)( pnlWhiteColourPreview.getPreferredSize( ).height * menuScaling ) ) );
        btnPrimaryColour1Change.setPreferredSize( new Dimension( (int)( btnPrimaryColour1Change.getPreferredSize( ).width * menuScaling ), (int)( btnPrimaryColour1Change.getPreferredSize( ).height * menuScaling ) ) );
        btnPrimaryColour2Change.setPreferredSize( new Dimension( (int)( btnPrimaryColour2Change.getPreferredSize( ).width * menuScaling ), (int)( btnPrimaryColour2Change.getPreferredSize( ).height * menuScaling ) ) );
        btnSecondaryColour1Change.setPreferredSize( new Dimension( (int)( btnSecondaryColour1Change.getPreferredSize( ).width * menuScaling ), (int)( btnSecondaryColour1Change.getPreferredSize( ).height * menuScaling ) ) );
        btnSecondaryColour2Change.setPreferredSize( new Dimension( (int)( btnSecondaryColour2Change.getPreferredSize( ).width * menuScaling ), (int)( btnSecondaryColour2Change.getPreferredSize( ).height * menuScaling ) ) );
        btnSecondaryColour3Change.setPreferredSize( new Dimension( (int)( btnSecondaryColour3Change.getPreferredSize( ).width * menuScaling ), (int)( btnSecondaryColour3Change.getPreferredSize( ).height * menuScaling ) ) );
        btnBlackColourChange.setPreferredSize( new Dimension( (int)( btnBlackColourChange.getPreferredSize( ).width * menuScaling ), (int)( btnBlackColourChange.getPreferredSize( ).height * menuScaling ) ) );
        btnWhiteColourChange.setPreferredSize( new Dimension( (int)( btnWhiteColourChange.getPreferredSize( ).width * menuScaling ), (int)( btnWhiteColourChange.getPreferredSize( ).height * menuScaling ) ) );
        sldMenuOpacity.setPreferredSize( new Dimension( (int)( sldMenuOpacity.getPreferredSize( ).width * menuScaling ), (int)( sldMenuOpacity.getPreferredSize( ).height * menuScaling ) ) );
        btnChangeFont.setPreferredSize( new Dimension( (int)( btnChangeFont.getPreferredSize( ).width * menuScaling ), (int)( btnChangeFont.getPreferredSize( ).height * menuScaling ) ) );
        btnReset.setPreferredSize( new Dimension( (int)( btnReset.getPreferredSize( ).width * menuScaling ), (int)( btnReset.getPreferredSize( ).height * menuScaling ) ) );
        pnlThemeButtons.setPreferredSize( new Dimension( pnlThemeButtons.getPreferredSize( ).width, btnReset.getPreferredSize( ).height + 6 ) );
        spnWindowWidth.setPreferredSize( new Dimension( (int)( spnWindowWidth.getPreferredSize( ).width * menuScaling ), (int)( spnWindowWidth.getPreferredSize( ).height * menuScaling ) ) );
        spnWindowHeight.setPreferredSize( new Dimension( (int)( spnWindowHeight.getPreferredSize( ).width * menuScaling ), (int)( spnWindowHeight.getPreferredSize( ).height * menuScaling ) ) );
        txtBackgroundColour.setPreferredSize( new Dimension( (int)( txtBackgroundColour.getPreferredSize( ).width * menuScaling ), (int)( txtBackgroundColour.getPreferredSize( ).height * menuScaling ) ) );
        pnlBackgroundPreviewContainer.setPreferredSize( new Dimension( (int)( pnlBackgroundPreviewContainer.getPreferredSize( ).width * menuScaling ), (int)( pnlBackgroundPreviewContainer.getPreferredSize( ).height * menuScaling ) ) );
        pnlBackgroundPreview.setPreferredSize( new Dimension( (int)( pnlBackgroundPreview.getPreferredSize( ).width * menuScaling ), (int)( pnlBackgroundPreview.getPreferredSize( ).height * menuScaling ) ) );
        btnBackgroundColourChange.setPreferredSize( new Dimension( (int)( btnBackgroundColourChange.getPreferredSize( ).width * menuScaling ), (int)( btnBackgroundColourChange.getPreferredSize( ).height * menuScaling ) ) );
        btnBackgroundImageChange.setPreferredSize( new Dimension( (int)( btnBackgroundImageChange.getPreferredSize( ).width * menuScaling ), (int)( btnBackgroundImageChange.getPreferredSize( ).height * menuScaling ) ) );
        btnBackgroundImageRemove.setPreferredSize( new Dimension( (int)( btnBackgroundImageRemove.getPreferredSize( ).width * menuScaling ), (int)( btnBackgroundImageRemove.getPreferredSize( ).height * menuScaling ) ) );
        cmbBackgroundImageMode.setPreferredSize( btnBackgroundImageRemove.getPreferredSize( ) );
        pnlBackgroundImage.setPreferredSize( new Dimension( (int)( pnlBackgroundImage.getPreferredSize( ).width * menuScaling ), (int)( pnlBackgroundImage.getPreferredSize( ).height * menuScaling ) ) );
        pnlBackgroundImage.setMaximumSize( pnlBackgroundImage.getPreferredSize( ) );
        lblIcon.setPreferredSize( new Dimension( (int)( lblIcon.getPreferredSize( ).width * menuScaling ), (int)( lblIcon.getPreferredSize( ).height * menuScaling ) ) );
        lblIcon.setMaximumSize( lblIcon.getPreferredSize( ) );
        if( getIconImages( ).size( ) > 0 )
            lblIcon.setIcon( new ImageIcon( getIconImages( ).get( 0 ).getScaledInstance( lblIcon.getPreferredSize( ).width, lblIcon.getPreferredSize( ).height, Image.SCALE_DEFAULT ) ) );
        btnWebsite.setPreferredSize( new Dimension( (int)( btnWebsite.getPreferredSize( ).width * menuScaling ), (int)( btnWebsite.getPreferredSize( ).height * menuScaling ) ) );
        btnDiscord.setPreferredSize( new Dimension( (int)( btnDiscord.getPreferredSize( ).width * menuScaling ), (int)( btnDiscord.getPreferredSize( ).height * menuScaling ) ) );
        btnPatreon.setPreferredSize( new Dimension( (int)( btnPatreon.getPreferredSize( ).width * menuScaling ), (int)( btnPatreon.getPreferredSize( ).height * menuScaling ) ) );
        pnlAboutButtons.setPreferredSize( new Dimension( pnlAboutButtons.getPreferredSize( ).width, btnWebsite.getPreferredSize( ).height + 6 ) );
        btnDone.setPreferredSize( new Dimension( (int)( btnDone.getPreferredSize( ).width * menuScaling ), (int)( btnDone.getPreferredSize( ).height * menuScaling ) ) );
        btnCancel.setPreferredSize( new Dimension( (int)( btnCancel.getPreferredSize( ).width * menuScaling ), (int)( btnCancel.getPreferredSize( ).height * menuScaling ) ) );
        pnlFooter.setPreferredSize( new Dimension( pnlFooter.getPreferredSize( ).width, btnDone.getPreferredSize( ).height + 6 ) );
        pack( );
        suppressTextChanged = false;
        setVisible( true );
        suppressTextChanged = true;
        
        return true;
    }
    
    private void browseToUrl( String url )
    {
        try
        {
            Desktop desktop = Desktop.isDesktopSupported( ) ? Desktop.getDesktop( ) : null;
            if( desktop != null && desktop.isSupported( Desktop.Action.BROWSE ) )
                desktop.browse( new URI( url ) );
            else
                throw new UnsupportedOperationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedOpenWebBrowserErrorMessage" ) + " " + url );
        }
        catch( Exception e )
        {
            JOptionPane.showMessageDialog( this, e.toString( ), "Error", JOptionPane.PLAIN_MESSAGE );
        }
    }
    
    public boolean getEnvironmentReloadRequired( )
    {
        return environmentReloadRequired;
    }
    
    public boolean getImageReloadRequired( )
    {
        return imageReloadRequired;
    }
    
    public boolean getInteractiveWindowReloadRequired( )
    {
        return interactiveWindowReloadRequired;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings( "unchecked" )
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        grpFilter = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        pnlTabs = new javax.swing.JTabbedPane();
        pnlGeneral = new JPanel();
        chkAlwaysShowShimejiChooser = new javax.swing.JCheckBox();
        lblScaling = new JLabel();
        sldScaling = new javax.swing.JSlider();
        lblFilter = new JLabel();
        radFilterNearest = new javax.swing.JRadioButton();
        radFilterBicubic = new javax.swing.JRadioButton();
        radFilterHqx = new javax.swing.JRadioButton();
        sldOpacity = new javax.swing.JSlider();
        lblOpacity = new JLabel();
        chkAlwaysShowInformationScreen = new javax.swing.JCheckBox();
        pnlInteractiveWindows = new JPanel();
        pnlInteractiveTabs = new javax.swing.JTabbedPane();
        pnlWhitelistTab = new JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstInteractiveWindows = new javax.swing.JList();
        pnlBlacklistTab = new JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstInteractiveWindowsBlacklist = new javax.swing.JList();
        pnlInteractiveButtons = new JPanel();
        btnAddInteractiveWindow = new javax.swing.JButton();
        btnRemoveInteractiveWindow = new javax.swing.JButton();
        pnlTheme = new JPanel();
        pnlThemeButtons = new JPanel();
        btnChangeFont = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        lblPrimaryColour1 = new JLabel();
        txtPrimaryColour1 = new JTextField();
        pnlPrimaryColour1PreviewContainer = new JPanel();
        gluePrimaryColour1a = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlPrimaryColour1Preview = new JPanel();
        gluePrimaryColour1b = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnPrimaryColour1Change = new javax.swing.JButton();
        lblPrimaryColour2 = new JLabel();
        txtPrimaryColour2 = new JTextField();
        pnlPrimaryColour2PreviewContainer = new JPanel();
        gluePrimaryColour2a = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlPrimaryColour2Preview = new JPanel();
        gluePrimaryColour2b = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnPrimaryColour2Change = new javax.swing.JButton();
        lblSecondaryColour1 = new JLabel();
        txtSecondaryColour1 = new JTextField();
        pnlSecondaryColour1PreviewContainer = new JPanel();
        glueSecondaryColour1a = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlSecondaryColour1Preview = new JPanel();
        glueSecondaryColour1b = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnSecondaryColour1Change = new javax.swing.JButton();
        lblSecondaryColour2 = new JLabel();
        txtSecondaryColour2 = new JTextField();
        pnlSecondaryColour2PreviewContainer = new JPanel();
        glueSecondaryColour2a = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlSecondaryColour2Preview = new JPanel();
        glueSecondaryColour2b = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnSecondaryColour2Change = new javax.swing.JButton();
        lblSecondaryColour3 = new JLabel();
        txtSecondaryColour3 = new JTextField();
        pnlSecondaryColour3PreviewContainer = new JPanel();
        glueSecondaryColour3a = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlSecondaryColour3Preview = new JPanel();
        glueSecondaryColour3b = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnSecondaryColour3Change = new javax.swing.JButton();
        lblMenuOpacity = new JLabel();
        sldMenuOpacity = new javax.swing.JSlider();
        lblBlackColour = new JLabel();
        txtBlackColour = new JTextField();
        pnlBlackColourPreviewContainer = new JPanel();
        glueBlackColoura = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlBlackColourPreview = new JPanel();
        glueBlackColourb = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnBlackColourChange = new javax.swing.JButton();
        lblWhiteColour = new JLabel();
        txtWhiteColour = new JTextField();
        pnlWhiteColourPreviewContainer = new JPanel();
        glueWhiteColoura = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlWhiteColourPreview = new JPanel();
        glueWhiteColourb = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        btnWhiteColourChange = new javax.swing.JButton();
        pnlScrollPane = new javax.swing.JScrollPane();
        pnlEditorPane = new javax.swing.JEditorPane();
        pnlWindowMode = new JPanel();
        chkWindowModeEnabled = new javax.swing.JCheckBox();
        lblDimensions = new JLabel();
        lblDimensionsX = new JLabel();
        lblBackground = new JLabel();
        txtBackgroundColour = new JTextField();
        btnBackgroundColourChange = new javax.swing.JButton();
        spnWindowWidth = new javax.swing.JSpinner();
        spnWindowHeight = new javax.swing.JSpinner();
        lblBackgroundColour = new JLabel();
        pnlBackgroundPreviewContainer = new JPanel();
        glueBackground = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlBackgroundPreview = new JPanel();
        glueBackground2 = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 0));
        lblBackgroundImageCaption = new JLabel();
        pnlBackgroundImage = new JPanel();
        lblBackgroundImage = new JLabel();
        btnBackgroundImageChange = new javax.swing.JButton();
        btnBackgroundImageRemove = new javax.swing.JButton();
        cmbBackgroundImageMode = new javax.swing.JComboBox<String>();
        pnlAbout = new JPanel();
        glue1 = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        lblIcon = new JLabel();
        rigid1 = new javax.swing.Box.Filler(new Dimension(0, 15), new Dimension(0, 15), new Dimension(0, 15));
        lblShimejiEE = new JLabel();
        rigid2 = new javax.swing.Box.Filler(new Dimension(0, 10), new Dimension(0, 5), new Dimension(0, 10));
        lblVersion = new JLabel();
        rigid3 = new javax.swing.Box.Filler(new Dimension(0, 15), new Dimension(0, 15), new Dimension(0, 15));
        lblDevelopedBy = new JLabel();
        lblKilkakon = new JLabel();
        rigid4 = new javax.swing.Box.Filler(new Dimension(0, 30), new Dimension(0, 30), new Dimension(0, 30));
        pnlAboutButtons = new JPanel();
        btnWebsite = new javax.swing.JButton();
        btnDiscord = new javax.swing.JButton();
        btnPatreon = new javax.swing.JButton();
        glue2 = new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(0, 32767));
        pnlFooter = new JPanel();
        btnDone = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        chkAlwaysShowShimejiChooser.setText("Always Show Shimeji Chooser");
        chkAlwaysShowShimejiChooser.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                chkAlwaysShowShimejiChooserItemStateChanged(evt);
            }
        });

        lblScaling.setText("Scaling");

        sldScaling.setMajorTickSpacing(10);
        sldScaling.setMaximum(80);
        sldScaling.setMinorTickSpacing(5);
        sldScaling.setPaintLabels(true);
        sldScaling.setPaintTicks(true);
        sldScaling.setSnapToTicks(true);
        sldScaling.setValue(10);
        sldScaling.setPreferredSize(new Dimension(300, 45));
        sldScaling.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                sldScalingStateChanged(evt);
            }
        });

        lblFilter.setText("Filter");

        radFilterNearest.setText("Nearest");
        radFilterNearest.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                radFilterItemStateChanged(evt);
            }
        });

        radFilterBicubic.setText("Bicubic");
        radFilterBicubic.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                radFilterItemStateChanged(evt);
            }
        });

        radFilterHqx.setText("hqx");
        radFilterHqx.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                radFilterItemStateChanged(evt);
            }
        });

        sldOpacity.setMajorTickSpacing(10);
        sldOpacity.setMinorTickSpacing(5);
        sldOpacity.setPaintLabels(true);
        sldOpacity.setPaintTicks(true);
        sldOpacity.setSnapToTicks(true);
        sldOpacity.setValue(10);
        sldOpacity.setPreferredSize(new Dimension(300, 45));
        sldOpacity.addChangeListener(new javax.swing.event.ChangeListener()
        {
            public void stateChanged(javax.swing.event.ChangeEvent evt)
            {
                sldOpacityStateChanged(evt);
            }
        });

        lblOpacity.setText("Opacity");

        chkAlwaysShowInformationScreen.setText("Always Show Information Screen");
        chkAlwaysShowInformationScreen.addItemListener(new java.awt.event.ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                chkAlwaysShowInformationScreenItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout pnlGeneralLayout = new javax.swing.GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(
            pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(chkAlwaysShowShimejiChooser)
                    .addComponent(lblFilter)
                    .addComponent(lblScaling)
                    .addGroup(pnlGeneralLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(sldOpacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(radFilterNearest)
                                .addComponent(sldScaling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(radFilterBicubic)
                                .addComponent(radFilterHqx))))
                    .addComponent(lblOpacity)
                    .addComponent(chkAlwaysShowInformationScreen))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        pnlGeneralLayout.setVerticalGroup(
            pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkAlwaysShowShimejiChooser)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkAlwaysShowInformationScreen)
                .addGap(18, 18, 18)
                .addComponent(lblOpacity)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldOpacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblScaling)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sldScaling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblFilter)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radFilterNearest)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radFilterBicubic)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(radFilterHqx)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        pnlTabs.addTab("General", pnlGeneral);

        lstInteractiveWindows.setModel(new javax.swing.AbstractListModel()
        {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(lstInteractiveWindows);

        javax.swing.GroupLayout pnlWhitelistTabLayout = new javax.swing.GroupLayout(pnlWhitelistTab);
        pnlWhitelistTab.setLayout(pnlWhitelistTabLayout);
        pnlWhitelistTabLayout.setHorizontalGroup(
            pnlWhitelistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlWhitelistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlWhitelistTabLayout.setVerticalGroup(
            pnlWhitelistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlWhitelistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                .addContainerGap())
        );

        pnlInteractiveTabs.addTab("Whitelist", pnlWhitelistTab);

        lstInteractiveWindowsBlacklist.setModel(new javax.swing.AbstractListModel()
        {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(lstInteractiveWindowsBlacklist);

        javax.swing.GroupLayout pnlBlacklistTabLayout = new javax.swing.GroupLayout(pnlBlacklistTab);
        pnlBlacklistTab.setLayout(pnlBlacklistTabLayout);
        pnlBlacklistTabLayout.setHorizontalGroup(
            pnlBlacklistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBlacklistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE)
                .addContainerGap())
        );
        pnlBlacklistTabLayout.setVerticalGroup(
            pnlBlacklistTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlBlacklistTabLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                .addContainerGap())
        );

        pnlInteractiveTabs.addTab("Blacklist", pnlBlacklistTab);

        pnlInteractiveButtons.setPreferredSize(new Dimension(380, 36));
        pnlInteractiveButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnAddInteractiveWindow.setText("Add");
        btnAddInteractiveWindow.setMaximumSize(new Dimension(130, 26));
        btnAddInteractiveWindow.setMinimumSize(new Dimension(95, 23));
        btnAddInteractiveWindow.setName(""); // NOI18N
        btnAddInteractiveWindow.setPreferredSize(new Dimension(130, 26));
        btnAddInteractiveWindow.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnAddInteractiveWindowActionPerformed(evt);
            }
        });
        pnlInteractiveButtons.add(btnAddInteractiveWindow);

        btnRemoveInteractiveWindow.setText("Remove");
        btnRemoveInteractiveWindow.setMaximumSize(new Dimension(130, 26));
        btnRemoveInteractiveWindow.setMinimumSize(new Dimension(95, 23));
        btnRemoveInteractiveWindow.setPreferredSize(new Dimension(130, 26));
        btnRemoveInteractiveWindow.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnRemoveInteractiveWindowActionPerformed(evt);
            }
        });
        pnlInteractiveButtons.add(btnRemoveInteractiveWindow);

        javax.swing.GroupLayout pnlInteractiveWindowsLayout = new javax.swing.GroupLayout(pnlInteractiveWindows);
        pnlInteractiveWindows.setLayout(pnlInteractiveWindowsLayout);
        pnlInteractiveWindowsLayout.setHorizontalGroup(
            pnlInteractiveWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlInteractiveWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlInteractiveWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlInteractiveButtons, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(pnlInteractiveTabs))
                .addContainerGap())
        );
        pnlInteractiveWindowsLayout.setVerticalGroup(
            pnlInteractiveWindowsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlInteractiveWindowsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlInteractiveTabs)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlInteractiveButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pnlTabs.addTab("InteractiveWindows", pnlInteractiveWindows);

        pnlThemeButtons.setPreferredSize(new Dimension(380, 36));
        pnlThemeButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnChangeFont.setText("Change Font");
        btnChangeFont.setMaximumSize(new Dimension(130, 26));
        btnChangeFont.setName(""); // NOI18N
        btnChangeFont.setPreferredSize(new Dimension(130, 26));
        btnChangeFont.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnChangeFontActionPerformed(evt);
            }
        });
        pnlThemeButtons.add(btnChangeFont);

        btnReset.setText("Reset");
        btnReset.setMaximumSize(new Dimension(130, 26));
        btnReset.setMinimumSize(new Dimension(95, 23));
        btnReset.setPreferredSize(new Dimension(130, 26));
        btnReset.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnResetActionPerformed(evt);
            }
        });
        pnlThemeButtons.add(btnReset);

        lblPrimaryColour1.setText("Primary 1");

        txtPrimaryColour1.setText("#00FF00");
        txtPrimaryColour1.setPreferredSize(new Dimension(70, 24));
        txtPrimaryColour1.getDocument( ).addDocumentListener( new DocumentListener( )
            {
                @Override
                public void insertUpdate( DocumentEvent e )
                {
                    colourTextChanged( txtPrimaryColour1 );
                }
                @Override
                public void removeUpdate( DocumentEvent e )
                {
                    colourTextChanged( txtPrimaryColour1 );
                }
                @Override
                public void changedUpdate( DocumentEvent e )
                {
                    colourTextChanged( txtPrimaryColour1 );
                }
            } );
            ( (AbstractDocument)txtPrimaryColour1.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                {
                    public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                    {
                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                        super.insertString( filterBypass, offset, text, attributes );
                    }

                    public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                    {
                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                        super.replace( filterBypass, offset, length, text, attributes );
                    }
                } );
                txtPrimaryColour1.addFocusListener(new java.awt.event.FocusAdapter()
                {
                    public void focusGained(java.awt.event.FocusEvent evt)
                    {
                        txtPrimaryColour1FocusGained(evt);
                    }
                    public void focusLost(java.awt.event.FocusEvent evt)
                    {
                        txtPrimaryColour1FocusLost(evt);
                    }
                });

                pnlPrimaryColour1PreviewContainer.setLayout(new javax.swing.BoxLayout(pnlPrimaryColour1PreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                pnlPrimaryColour1PreviewContainer.add(gluePrimaryColour1a);

                pnlPrimaryColour1Preview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                pnlPrimaryColour1Preview.setPreferredSize(new Dimension(20, 20));

                javax.swing.GroupLayout pnlPrimaryColour1PreviewLayout = new javax.swing.GroupLayout(pnlPrimaryColour1Preview);
                pnlPrimaryColour1Preview.setLayout(pnlPrimaryColour1PreviewLayout);
                pnlPrimaryColour1PreviewLayout.setHorizontalGroup(
                    pnlPrimaryColour1PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGap(0, 0, Short.MAX_VALUE)
                );
                pnlPrimaryColour1PreviewLayout.setVerticalGroup(
                    pnlPrimaryColour1PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGap(0, 0, Short.MAX_VALUE)
                );

                pnlPrimaryColour1PreviewContainer.add(pnlPrimaryColour1Preview);
                pnlPrimaryColour1PreviewContainer.add(gluePrimaryColour1b);

                btnPrimaryColour1Change.setText("Change");
                btnPrimaryColour1Change.addActionListener(new java.awt.event.ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent evt)
                    {
                        btnPrimaryColour1ChangeActionPerformed(evt);
                    }
                });

                lblPrimaryColour2.setText("Primary 2");

                txtPrimaryColour2.setText("#00FF00");
                txtPrimaryColour2.setPreferredSize(new Dimension(70, 24));
                txtPrimaryColour2.getDocument( ).addDocumentListener( new DocumentListener( )
                    {
                        @Override
                        public void insertUpdate( DocumentEvent e )
                        {
                            colourTextChanged( txtPrimaryColour2 );
                        }
                        @Override
                        public void removeUpdate( DocumentEvent e )
                        {
                            colourTextChanged( txtPrimaryColour2 );
                        }
                        @Override
                        public void changedUpdate( DocumentEvent e )
                        {
                            colourTextChanged( txtPrimaryColour2 );
                        }
                    } );
                    ( (AbstractDocument)txtPrimaryColour2.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                        {
                            public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                            {
                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                super.insertString( filterBypass, offset, text, attributes );
                            }

                            public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                            {
                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                super.replace( filterBypass, offset, length, text, attributes );
                            }
                        } );
                        txtPrimaryColour2.addFocusListener(new java.awt.event.FocusAdapter()
                        {
                            public void focusGained(java.awt.event.FocusEvent evt)
                            {
                                txtPrimaryColour2FocusGained(evt);
                            }
                            public void focusLost(java.awt.event.FocusEvent evt)
                            {
                                txtPrimaryColour2FocusLost(evt);
                            }
                        });

                        pnlPrimaryColour2PreviewContainer.setLayout(new javax.swing.BoxLayout(pnlPrimaryColour2PreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                        pnlPrimaryColour2PreviewContainer.add(gluePrimaryColour2a);

                        pnlPrimaryColour2Preview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                        pnlPrimaryColour2Preview.setPreferredSize(new Dimension(20, 20));

                        javax.swing.GroupLayout pnlPrimaryColour2PreviewLayout = new javax.swing.GroupLayout(pnlPrimaryColour2Preview);
                        pnlPrimaryColour2Preview.setLayout(pnlPrimaryColour2PreviewLayout);
                        pnlPrimaryColour2PreviewLayout.setHorizontalGroup(
                            pnlPrimaryColour2PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGap(0, 0, Short.MAX_VALUE)
                        );
                        pnlPrimaryColour2PreviewLayout.setVerticalGroup(
                            pnlPrimaryColour2PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGap(0, 0, Short.MAX_VALUE)
                        );

                        pnlPrimaryColour2PreviewContainer.add(pnlPrimaryColour2Preview);
                        pnlPrimaryColour2PreviewContainer.add(gluePrimaryColour2b);

                        btnPrimaryColour2Change.setText("Change");
                        btnPrimaryColour2Change.addActionListener(new java.awt.event.ActionListener()
                        {
                            public void actionPerformed(java.awt.event.ActionEvent evt)
                            {
                                btnPrimaryColour2ChangeActionPerformed(evt);
                            }
                        });

                        lblSecondaryColour1.setText("Secondary 1");

                        txtSecondaryColour1.setText("#00FF00");
                        txtSecondaryColour1.setPreferredSize(new Dimension(70, 24));
                        txtSecondaryColour1.getDocument( ).addDocumentListener( new DocumentListener( )
                            {
                                @Override
                                public void insertUpdate( DocumentEvent e )
                                {
                                    colourTextChanged( txtSecondaryColour1 );
                                }
                                @Override
                                public void removeUpdate( DocumentEvent e )
                                {
                                    colourTextChanged( txtSecondaryColour1 );
                                }
                                @Override
                                public void changedUpdate( DocumentEvent e )
                                {
                                    colourTextChanged( txtSecondaryColour1 );
                                }
                            } );
                            ( (AbstractDocument)txtSecondaryColour1.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                                {
                                    public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                                    {
                                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                        super.insertString( filterBypass, offset, text, attributes );
                                    }

                                    public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                                    {
                                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                        super.replace( filterBypass, offset, length, text, attributes );
                                    }
                                } );
                                txtSecondaryColour1.addFocusListener(new java.awt.event.FocusAdapter()
                                {
                                    public void focusGained(java.awt.event.FocusEvent evt)
                                    {
                                        txtSecondaryColour1FocusGained(evt);
                                    }
                                    public void focusLost(java.awt.event.FocusEvent evt)
                                    {
                                        txtSecondaryColour1FocusLost(evt);
                                    }
                                });

                                pnlSecondaryColour1PreviewContainer.setLayout(new javax.swing.BoxLayout(pnlSecondaryColour1PreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                                pnlSecondaryColour1PreviewContainer.add(glueSecondaryColour1a);

                                pnlSecondaryColour1Preview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                pnlSecondaryColour1Preview.setPreferredSize(new Dimension(20, 20));

                                javax.swing.GroupLayout pnlSecondaryColour1PreviewLayout = new javax.swing.GroupLayout(pnlSecondaryColour1Preview);
                                pnlSecondaryColour1Preview.setLayout(pnlSecondaryColour1PreviewLayout);
                                pnlSecondaryColour1PreviewLayout.setHorizontalGroup(
                                    pnlSecondaryColour1PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGap(0, 0, Short.MAX_VALUE)
                                );
                                pnlSecondaryColour1PreviewLayout.setVerticalGroup(
                                    pnlSecondaryColour1PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGap(0, 0, Short.MAX_VALUE)
                                );

                                pnlSecondaryColour1PreviewContainer.add(pnlSecondaryColour1Preview);
                                pnlSecondaryColour1PreviewContainer.add(glueSecondaryColour1b);

                                btnSecondaryColour1Change.setText("Change");
                                btnSecondaryColour1Change.addActionListener(new java.awt.event.ActionListener()
                                {
                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                    {
                                        btnSecondaryColour1ChangeActionPerformed(evt);
                                    }
                                });

                                lblSecondaryColour2.setText("Secondary 2");

                                txtSecondaryColour2.setText("#00FF00");
                                txtSecondaryColour2.setPreferredSize(new Dimension(70, 24));
                                txtSecondaryColour2.getDocument( ).addDocumentListener( new DocumentListener( )
                                    {
                                        @Override
                                        public void insertUpdate( DocumentEvent e )
                                        {
                                            colourTextChanged( txtSecondaryColour2 );
                                        }
                                        @Override
                                        public void removeUpdate( DocumentEvent e )
                                        {
                                            colourTextChanged( txtSecondaryColour2 );
                                        }
                                        @Override
                                        public void changedUpdate( DocumentEvent e )
                                        {
                                            colourTextChanged( txtSecondaryColour2 );
                                        }
                                    } );
                                    ( (AbstractDocument)txtSecondaryColour2.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                                        {
                                            public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                                            {
                                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                                super.insertString( filterBypass, offset, text, attributes );
                                            }

                                            public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                                            {
                                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                                super.replace( filterBypass, offset, length, text, attributes );
                                            }
                                        } );
                                        txtSecondaryColour2.addFocusListener(new java.awt.event.FocusAdapter()
                                        {
                                            public void focusGained(java.awt.event.FocusEvent evt)
                                            {
                                                txtSecondaryColour2FocusGained(evt);
                                            }
                                            public void focusLost(java.awt.event.FocusEvent evt)
                                            {
                                                txtSecondaryColour2FocusLost(evt);
                                            }
                                        });

                                        pnlSecondaryColour2PreviewContainer.setLayout(new javax.swing.BoxLayout(pnlSecondaryColour2PreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                                        pnlSecondaryColour2PreviewContainer.add(glueSecondaryColour2a);

                                        pnlSecondaryColour2Preview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                        pnlSecondaryColour2Preview.setPreferredSize(new Dimension(20, 20));

                                        javax.swing.GroupLayout pnlSecondaryColour2PreviewLayout = new javax.swing.GroupLayout(pnlSecondaryColour2Preview);
                                        pnlSecondaryColour2Preview.setLayout(pnlSecondaryColour2PreviewLayout);
                                        pnlSecondaryColour2PreviewLayout.setHorizontalGroup(
                                            pnlSecondaryColour2PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGap(0, 0, Short.MAX_VALUE)
                                        );
                                        pnlSecondaryColour2PreviewLayout.setVerticalGroup(
                                            pnlSecondaryColour2PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGap(0, 0, Short.MAX_VALUE)
                                        );

                                        pnlSecondaryColour2PreviewContainer.add(pnlSecondaryColour2Preview);
                                        pnlSecondaryColour2PreviewContainer.add(glueSecondaryColour2b);

                                        btnSecondaryColour2Change.setText("Change");
                                        btnSecondaryColour2Change.addActionListener(new java.awt.event.ActionListener()
                                        {
                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                            {
                                                btnSecondaryColour2ChangeActionPerformed(evt);
                                            }
                                        });

                                        lblSecondaryColour3.setText("Secondary 3");

                                        txtSecondaryColour3.setText("#00FF00");
                                        txtSecondaryColour3.setPreferredSize(new Dimension(70, 24));
                                        txtSecondaryColour3.getDocument( ).addDocumentListener( new DocumentListener( )
                                            {
                                                @Override
                                                public void insertUpdate( DocumentEvent e )
                                                {
                                                    colourTextChanged( txtSecondaryColour3 );
                                                }
                                                @Override
                                                public void removeUpdate( DocumentEvent e )
                                                {
                                                    colourTextChanged( txtSecondaryColour3 );
                                                }
                                                @Override
                                                public void changedUpdate( DocumentEvent e )
                                                {
                                                    colourTextChanged( txtSecondaryColour3 );
                                                }
                                            } );
                                            ( (AbstractDocument)txtSecondaryColour3.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                                                {
                                                    public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                                                    {
                                                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                                        super.insertString( filterBypass, offset, text, attributes );
                                                    }

                                                    public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                                                    {
                                                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                                        super.replace( filterBypass, offset, length, text, attributes );
                                                    }
                                                } );
                                                txtSecondaryColour3.addFocusListener(new java.awt.event.FocusAdapter()
                                                {
                                                    public void focusGained(java.awt.event.FocusEvent evt)
                                                    {
                                                        txtSecondaryColour3FocusGained(evt);
                                                    }
                                                    public void focusLost(java.awt.event.FocusEvent evt)
                                                    {
                                                        txtSecondaryColour3FocusLost(evt);
                                                    }
                                                });

                                                pnlSecondaryColour3PreviewContainer.setLayout(new javax.swing.BoxLayout(pnlSecondaryColour3PreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                                                pnlSecondaryColour3PreviewContainer.add(glueSecondaryColour3a);

                                                pnlSecondaryColour3Preview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                                pnlSecondaryColour3Preview.setPreferredSize(new Dimension(20, 20));

                                                javax.swing.GroupLayout pnlSecondaryColour3PreviewLayout = new javax.swing.GroupLayout(pnlSecondaryColour3Preview);
                                                pnlSecondaryColour3Preview.setLayout(pnlSecondaryColour3PreviewLayout);
                                                pnlSecondaryColour3PreviewLayout.setHorizontalGroup(
                                                    pnlSecondaryColour3PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGap(0, 0, Short.MAX_VALUE)
                                                );
                                                pnlSecondaryColour3PreviewLayout.setVerticalGroup(
                                                    pnlSecondaryColour3PreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addGap(0, 0, Short.MAX_VALUE)
                                                );

                                                pnlSecondaryColour3PreviewContainer.add(pnlSecondaryColour3Preview);
                                                pnlSecondaryColour3PreviewContainer.add(glueSecondaryColour3b);

                                                btnSecondaryColour3Change.setText("Change");
                                                btnSecondaryColour3Change.addActionListener(new java.awt.event.ActionListener()
                                                {
                                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                                    {
                                                        btnSecondaryColour3ChangeActionPerformed(evt);
                                                    }
                                                });

                                                lblMenuOpacity.setText("Menu Opacity");

                                                sldMenuOpacity.setMajorTickSpacing(10);
                                                sldMenuOpacity.setMinorTickSpacing(5);
                                                sldMenuOpacity.setPaintLabels(true);
                                                sldMenuOpacity.setPaintTicks(true);
                                                sldMenuOpacity.setSnapToTicks(true);
                                                sldMenuOpacity.setValue(10);
                                                sldMenuOpacity.setPreferredSize(new Dimension(300, 45));
                                                sldMenuOpacity.addChangeListener(new javax.swing.event.ChangeListener()
                                                {
                                                    public void stateChanged(javax.swing.event.ChangeEvent evt)
                                                    {
                                                        sldMenuOpacityStateChanged(evt);
                                                    }
                                                });

                                                lblBlackColour.setText("Text");

                                                txtBlackColour.setText("#00FF00");
                                                txtBlackColour.setPreferredSize(new Dimension(70, 24));
                                                txtBlackColour.getDocument( ).addDocumentListener( new DocumentListener( )
                                                    {
                                                        @Override
                                                        public void insertUpdate( DocumentEvent e )
                                                        {
                                                            colourTextChanged( txtBlackColour );
                                                        }
                                                        @Override
                                                        public void removeUpdate( DocumentEvent e )
                                                        {
                                                            colourTextChanged( txtBlackColour );
                                                        }
                                                        @Override
                                                        public void changedUpdate( DocumentEvent e )
                                                        {
                                                            colourTextChanged( txtBlackColour );
                                                        }
                                                    } );
                                                    ( (AbstractDocument)txtBlackColour.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                                                        {
                                                            public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                                                            {
                                                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                                                super.insertString( filterBypass, offset, text, attributes );
                                                            }

                                                            public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                                                            {
                                                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                                                super.replace( filterBypass, offset, length, text, attributes );
                                                            }
                                                        } );
                                                        txtBlackColour.addFocusListener(new java.awt.event.FocusAdapter()
                                                        {
                                                            public void focusGained(java.awt.event.FocusEvent evt)
                                                            {
                                                                txtBlackColourFocusGained(evt);
                                                            }
                                                            public void focusLost(java.awt.event.FocusEvent evt)
                                                            {
                                                                txtBlackColourFocusLost(evt);
                                                            }
                                                        });

                                                        pnlBlackColourPreviewContainer.setLayout(new javax.swing.BoxLayout(pnlBlackColourPreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                                                        pnlBlackColourPreviewContainer.add(glueBlackColoura);

                                                        pnlBlackColourPreview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                                        pnlBlackColourPreview.setPreferredSize(new Dimension(20, 20));

                                                        javax.swing.GroupLayout pnlBlackColourPreviewLayout = new javax.swing.GroupLayout(pnlBlackColourPreview);
                                                        pnlBlackColourPreview.setLayout(pnlBlackColourPreviewLayout);
                                                        pnlBlackColourPreviewLayout.setHorizontalGroup(
                                                            pnlBlackColourPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addGap(0, 0, Short.MAX_VALUE)
                                                        );
                                                        pnlBlackColourPreviewLayout.setVerticalGroup(
                                                            pnlBlackColourPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addGap(0, 0, Short.MAX_VALUE)
                                                        );

                                                        pnlBlackColourPreviewContainer.add(pnlBlackColourPreview);
                                                        pnlBlackColourPreviewContainer.add(glueBlackColourb);

                                                        btnBlackColourChange.setText("Change");
                                                        btnBlackColourChange.addActionListener(new java.awt.event.ActionListener()
                                                        {
                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                            {
                                                                btnBlackColourChangeActionPerformed(evt);
                                                            }
                                                        });

                                                        lblWhiteColour.setText("Background");
                                                        lblWhiteColour.setToolTipText("");

                                                        txtWhiteColour.setText("#00FF00");
                                                        txtWhiteColour.setPreferredSize(new Dimension(70, 24));
                                                        txtWhiteColour.getDocument( ).addDocumentListener( new DocumentListener( )
                                                            {
                                                                @Override
                                                                public void insertUpdate( DocumentEvent e )
                                                                {
                                                                    colourTextChanged( txtWhiteColour );
                                                                }
                                                                @Override
                                                                public void removeUpdate( DocumentEvent e )
                                                                {
                                                                    colourTextChanged( txtWhiteColour );
                                                                }
                                                                @Override
                                                                public void changedUpdate( DocumentEvent e )
                                                                {
                                                                    colourTextChanged( txtWhiteColour );
                                                                }
                                                            } );
                                                            ( (AbstractDocument)txtWhiteColour.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                                                                {
                                                                    public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                                                                    {
                                                                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                                                        super.insertString( filterBypass, offset, text, attributes );
                                                                    }

                                                                    public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                                                                    {
                                                                        if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                                                        super.replace( filterBypass, offset, length, text, attributes );
                                                                    }
                                                                } );
                                                                txtWhiteColour.addFocusListener(new java.awt.event.FocusAdapter()
                                                                {
                                                                    public void focusGained(java.awt.event.FocusEvent evt)
                                                                    {
                                                                        txtWhiteColourFocusGained(evt);
                                                                    }
                                                                    public void focusLost(java.awt.event.FocusEvent evt)
                                                                    {
                                                                        txtWhiteColourFocusLost(evt);
                                                                    }
                                                                });

                                                                pnlWhiteColourPreviewContainer.setLayout(new javax.swing.BoxLayout(pnlWhiteColourPreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                                                                pnlWhiteColourPreviewContainer.add(glueWhiteColoura);

                                                                pnlWhiteColourPreview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                                                pnlWhiteColourPreview.setPreferredSize(new Dimension(20, 20));

                                                                javax.swing.GroupLayout pnlWhiteColourPreviewLayout = new javax.swing.GroupLayout(pnlWhiteColourPreview);
                                                                pnlWhiteColourPreview.setLayout(pnlWhiteColourPreviewLayout);
                                                                pnlWhiteColourPreviewLayout.setHorizontalGroup(
                                                                    pnlWhiteColourPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                    .addGap(0, 0, Short.MAX_VALUE)
                                                                );
                                                                pnlWhiteColourPreviewLayout.setVerticalGroup(
                                                                    pnlWhiteColourPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                    .addGap(0, 0, Short.MAX_VALUE)
                                                                );

                                                                pnlWhiteColourPreviewContainer.add(pnlWhiteColourPreview);
                                                                pnlWhiteColourPreviewContainer.add(glueWhiteColourb);

                                                                btnWhiteColourChange.setText("Change");
                                                                btnWhiteColourChange.addActionListener(new java.awt.event.ActionListener()
                                                                {
                                                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                    {
                                                                        btnWhiteColourChangeActionPerformed(evt);
                                                                    }
                                                                });

                                                                pnlScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

                                                                pnlEditorPane.setEditable(false);
                                                                pnlEditorPane.setContentType("text/html"); // NOI18N
                                                                pnlEditorPane.setRequestFocusEnabled(false);
                                                                pnlScrollPane.setViewportView(pnlEditorPane);

                                                                javax.swing.GroupLayout pnlThemeLayout = new javax.swing.GroupLayout(pnlTheme);
                                                                pnlTheme.setLayout(pnlThemeLayout);
                                                                pnlThemeLayout.setHorizontalGroup(
                                                                    pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                    .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                        .addContainerGap()
                                                                        .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                    .addComponent(pnlThemeButtons, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                                                    .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                        .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                            .addComponent(lblPrimaryColour1)
                                                                                            .addComponent(lblPrimaryColour2)
                                                                                            .addComponent(lblSecondaryColour1)
                                                                                            .addComponent(lblSecondaryColour2)
                                                                                            .addComponent(lblSecondaryColour3)
                                                                                            .addComponent(lblBlackColour)
                                                                                            .addComponent(lblWhiteColour))
                                                                                        .addGap(18, 18, 18)
                                                                                        .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtSecondaryColour3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlSecondaryColour3PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(btnSecondaryColour3Change))
                                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, pnlThemeLayout.createSequentialGroup()
                                                                                                        .addComponent(txtWhiteColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                        .addComponent(pnlWhiteColourPreviewContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                    .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                        .addComponent(txtBlackColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                        .addComponent(pnlBlackColourPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                    .addComponent(btnBlackColourChange)
                                                                                                    .addComponent(btnWhiteColourChange, javax.swing.GroupLayout.Alignment.TRAILING)))
                                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtPrimaryColour2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlPrimaryColour2PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(btnPrimaryColour2Change))
                                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtSecondaryColour1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlSecondaryColour1PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(btnSecondaryColour1Change))
                                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtSecondaryColour2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlSecondaryColour2PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(btnSecondaryColour2Change))
                                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                                .addComponent(txtPrimaryColour1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlPrimaryColour1PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(btnPrimaryColour1Change)))
                                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                        .addComponent(pnlScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                                                                                .addContainerGap())
                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addComponent(lblMenuOpacity)
                                                                                    .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                        .addGap(10, 10, 10)
                                                                                        .addComponent(sldMenuOpacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                                .addGap(0, 26, Short.MAX_VALUE))))
                                                                );
                                                                pnlThemeLayout.setVerticalGroup(
                                                                    pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlThemeLayout.createSequentialGroup()
                                                                        .addContainerGap()
                                                                        .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                            .addGroup(pnlThemeLayout.createSequentialGroup()
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblPrimaryColour1)
                                                                                        .addComponent(txtPrimaryColour1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnPrimaryColour1Change))
                                                                                    .addComponent(pnlPrimaryColour1PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblPrimaryColour2)
                                                                                        .addComponent(txtPrimaryColour2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnPrimaryColour2Change))
                                                                                    .addComponent(pnlPrimaryColour2PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblSecondaryColour1)
                                                                                        .addComponent(txtSecondaryColour1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnSecondaryColour1Change))
                                                                                    .addComponent(pnlSecondaryColour1PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblSecondaryColour2)
                                                                                        .addComponent(txtSecondaryColour2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnSecondaryColour2Change))
                                                                                    .addComponent(pnlSecondaryColour2PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblSecondaryColour3)
                                                                                        .addComponent(txtSecondaryColour3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnSecondaryColour3Change))
                                                                                    .addComponent(pnlSecondaryColour3PreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblBlackColour)
                                                                                        .addComponent(txtBlackColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnBlackColourChange))
                                                                                    .addComponent(pnlBlackColourPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                                    .addGroup(pnlThemeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblWhiteColour)
                                                                                        .addComponent(txtWhiteColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnWhiteColourChange))
                                                                                    .addComponent(pnlWhiteColourPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                            .addComponent(pnlScrollPane))
                                                                        .addGap(18, 18, 18)
                                                                        .addComponent(lblMenuOpacity)
                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(sldMenuOpacity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(pnlThemeButtons, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addContainerGap())
                                                                );

                                                                pnlTabs.addTab("Theme", pnlTheme);

                                                                chkWindowModeEnabled.setText("Enable Windowed Mode");
                                                                chkWindowModeEnabled.addItemListener(new java.awt.event.ItemListener()
                                                                {
                                                                    public void itemStateChanged(ItemEvent evt)
                                                                    {
                                                                        chkWindowModeEnabledItemStateChanged(evt);
                                                                    }
                                                                });

                                                                lblDimensions.setText("Dimensions");

                                                                lblDimensionsX.setText("x");

                                                                lblBackground.setText("Background");

                                                                txtBackgroundColour.setText("#00FF00");
                                                                txtBackgroundColour.setPreferredSize(new Dimension(70, 24));
                                                                txtBackgroundColour.getDocument( ).addDocumentListener( new DocumentListener( )
                                                                    {
                                                                        @Override
                                                                        public void insertUpdate( DocumentEvent e )
                                                                        {
                                                                            colourTextChanged( txtBackgroundColour );
                                                                        }
                                                                        @Override
                                                                        public void removeUpdate( DocumentEvent e )
                                                                        {
                                                                            colourTextChanged( txtBackgroundColour );
                                                                        }
                                                                        @Override
                                                                        public void changedUpdate( DocumentEvent e )
                                                                        {
                                                                            colourTextChanged( txtBackgroundColour );
                                                                        }
                                                                    } );
                                                                    ( (AbstractDocument)txtBackgroundColour.getDocument( ) ).setDocumentFilter( new DocumentFilter( )
                                                                        {
                                                                            public void insertString( FilterBypass filterBypass, int offset, String text, AttributeSet attributes ) throws BadLocationException
                                                                            {
                                                                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) + text.length( ) <= 7 )
                                                                                super.insertString( filterBypass, offset, text, attributes );
                                                                            }

                                                                            public void replace( FilterBypass filterBypass, int offset, int length, String text, AttributeSet attributes ) throws BadLocationException
                                                                            {
                                                                                if( text != null && text.matches( "[#0-9a-fA-F]*" ) && filterBypass.getDocument( ).getLength( ) - length + text.length( ) <= 7 )
                                                                                super.replace( filterBypass, offset, length, text, attributes );
                                                                            }
                                                                        } );

                                                                        btnBackgroundColourChange.setText("Change");
                                                                        btnBackgroundColourChange.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnBackgroundColourChangeActionPerformed(evt);
                                                                            }
                                                                        });

                                                                        spnWindowWidth.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
                                                                        spnWindowWidth.setPreferredSize(new Dimension(60, 24));
                                                                        spnWindowWidth.addChangeListener(new javax.swing.event.ChangeListener()
                                                                        {
                                                                            public void stateChanged(javax.swing.event.ChangeEvent evt)
                                                                            {
                                                                                spnWindowWidthStateChanged(evt);
                                                                            }
                                                                        });

                                                                        spnWindowHeight.setModel(new SpinnerNumberModel(0, 0, 10000, 1));
                                                                        spnWindowHeight.setMinimumSize(new Dimension(30, 20));
                                                                        spnWindowHeight.setPreferredSize(new Dimension(60, 24));
                                                                        spnWindowHeight.addChangeListener(new javax.swing.event.ChangeListener()
                                                                        {
                                                                            public void stateChanged(javax.swing.event.ChangeEvent evt)
                                                                            {
                                                                                spnWindowHeightStateChanged(evt);
                                                                            }
                                                                        });

                                                                        lblBackgroundColour.setText("Colour");

                                                                        pnlBackgroundPreviewContainer.setLayout(new javax.swing.BoxLayout(pnlBackgroundPreviewContainer, javax.swing.BoxLayout.Y_AXIS));
                                                                        pnlBackgroundPreviewContainer.add(glueBackground);

                                                                        pnlBackgroundPreview.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                                                        pnlBackgroundPreview.setPreferredSize(new Dimension(20, 20));

                                                                        javax.swing.GroupLayout pnlBackgroundPreviewLayout = new javax.swing.GroupLayout(pnlBackgroundPreview);
                                                                        pnlBackgroundPreview.setLayout(pnlBackgroundPreviewLayout);
                                                                        pnlBackgroundPreviewLayout.setHorizontalGroup(
                                                                            pnlBackgroundPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGap(0, 0, Short.MAX_VALUE)
                                                                        );
                                                                        pnlBackgroundPreviewLayout.setVerticalGroup(
                                                                            pnlBackgroundPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGap(0, 0, Short.MAX_VALUE)
                                                                        );

                                                                        pnlBackgroundPreviewContainer.add(pnlBackgroundPreview);
                                                                        pnlBackgroundPreviewContainer.add(glueBackground2);

                                                                        lblBackgroundImageCaption.setText("Image");

                                                                        pnlBackgroundImage.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
                                                                        pnlBackgroundImage.setPreferredSize(new Dimension(96, 96));
                                                                        pnlBackgroundImage.setLayout(new java.awt.BorderLayout());

                                                                        lblBackgroundImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                                                                        pnlBackgroundImage.add(lblBackgroundImage, java.awt.BorderLayout.CENTER);

                                                                        btnBackgroundImageChange.setText("Change");
                                                                        btnBackgroundImageChange.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnBackgroundImageChangeActionPerformed(evt);
                                                                            }
                                                                        });

                                                                        btnBackgroundImageRemove.setText("Remove");
                                                                        btnBackgroundImageRemove.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnBackgroundImageRemoveActionPerformed(evt);
                                                                            }
                                                                        });

                                                                        cmbBackgroundImageMode.setModel(new javax.swing.DefaultComboBoxModel<String>());
                                                                        cmbBackgroundImageMode.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                cmbBackgroundImageModeActionPerformed(evt);
                                                                            }
                                                                        });

                                                                        javax.swing.GroupLayout pnlWindowModeLayout = new javax.swing.GroupLayout(pnlWindowMode);
                                                                        pnlWindowMode.setLayout(pnlWindowModeLayout);
                                                                        pnlWindowModeLayout.setHorizontalGroup(
                                                                            pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                .addContainerGap()
                                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addComponent(chkWindowModeEnabled)
                                                                                    .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                        .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                            .addComponent(lblDimensions)
                                                                                            .addComponent(lblBackground)
                                                                                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                                .addGap(10, 10, 10)
                                                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                    .addComponent(lblBackgroundImageCaption)
                                                                                                    .addComponent(lblBackgroundColour))))
                                                                                        .addGap(18, 18, 18)
                                                                                        .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                                .addComponent(spnWindowWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(lblDimensionsX)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(spnWindowHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                                .addComponent(txtBackgroundColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(pnlBackgroundPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(btnBackgroundColourChange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                                .addComponent(pnlBackgroundImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                                                    .addComponent(btnBackgroundImageRemove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                                    .addComponent(btnBackgroundImageChange, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                                    .addComponent(cmbBackgroundImageMode, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                                                                                .addContainerGap(85, Short.MAX_VALUE))
                                                                        );
                                                                        pnlWindowModeLayout.setVerticalGroup(
                                                                            pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                .addContainerGap()
                                                                                .addComponent(chkWindowModeEnabled)
                                                                                .addGap(18, 18, 18)
                                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                    .addComponent(lblDimensions)
                                                                                    .addComponent(lblDimensionsX)
                                                                                    .addComponent(spnWindowWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                    .addComponent(spnWindowHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addGap(18, 18, 18)
                                                                                .addComponent(lblBackground)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                        .addComponent(lblBackgroundColour)
                                                                                        .addComponent(txtBackgroundColour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addComponent(btnBackgroundColourChange))
                                                                                    .addComponent(pnlBackgroundPreviewContainer, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addGroup(pnlWindowModeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                        .addComponent(btnBackgroundImageChange)
                                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                        .addComponent(cmbBackgroundImageMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                        .addComponent(btnBackgroundImageRemove))
                                                                                    .addGroup(pnlWindowModeLayout.createSequentialGroup()
                                                                                        .addGap(4, 4, 4)
                                                                                        .addComponent(lblBackgroundImageCaption))
                                                                                    .addComponent(pnlBackgroundImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                .addContainerGap(115, Short.MAX_VALUE))
                                                                        );

                                                                        pnlTabs.addTab("WindowMode", pnlWindowMode);

                                                                        pnlAbout.setLayout(new javax.swing.BoxLayout(pnlAbout, javax.swing.BoxLayout.Y_AXIS));
                                                                        pnlAbout.add(glue1);

                                                                        lblIcon.setAlignmentX(0.5F);
                                                                        lblIcon.setMinimumSize(new Dimension(64, 64));
                                                                        lblIcon.setPreferredSize(new Dimension(64, 64));
                                                                        pnlAbout.add(lblIcon);
                                                                        pnlAbout.add(rigid1);

                                                                        lblShimejiEE.setFont(lblShimejiEE.getFont().deriveFont(lblShimejiEE.getFont().getStyle() | Font.BOLD, lblShimejiEE.getFont().getSize()+10));
                                                                        lblShimejiEE.setText("Shimeji");
                                                                        lblShimejiEE.setAlignmentX(0.5F);
                                                                        pnlAbout.add(lblShimejiEE);
                                                                        pnlAbout.add(rigid2);

                                                                        lblVersion.setFont(lblVersion.getFont().deriveFont(lblVersion.getFont().getSize()+4f));
                                                                        lblVersion.setText("1.0.22");
                                                                        lblVersion.setAlignmentX(0.5F);
                                                                        pnlAbout.add(lblVersion);
                                                                        pnlAbout.add(rigid3);

                                                                        lblDevelopedBy.setText("developed by");
                                                                        lblDevelopedBy.setAlignmentX(0.5F);
                                                                        pnlAbout.add(lblDevelopedBy);

                                                                        lblKilkakon.setText("Kilkakon");
                                                                        lblKilkakon.setAlignmentX(0.5F);
                                                                        pnlAbout.add(lblKilkakon);
                                                                        pnlAbout.add(rigid4);

                                                                        pnlAboutButtons.setMaximumSize(new Dimension(32767, 36));
                                                                        pnlAboutButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

                                                                        btnWebsite.setText("Website");
                                                                        btnWebsite.setAlignmentX(0.5F);
                                                                        btnWebsite.setMaximumSize(new Dimension(130, 26));
                                                                        btnWebsite.setPreferredSize(new Dimension(100, 26));
                                                                        btnWebsite.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnWebsiteActionPerformed(evt);
                                                                            }
                                                                        });
                                                                        pnlAboutButtons.add(btnWebsite);

                                                                        btnDiscord.setText("Discord");
                                                                        btnDiscord.setAlignmentX(0.5F);
                                                                        btnDiscord.setMaximumSize(new Dimension(130, 26));
                                                                        btnDiscord.setPreferredSize(new Dimension(100, 26));
                                                                        btnDiscord.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnDiscordActionPerformed(evt);
                                                                            }
                                                                        });
                                                                        pnlAboutButtons.add(btnDiscord);

                                                                        btnPatreon.setText("Patreon");
                                                                        btnPatreon.setAlignmentX(0.5F);
                                                                        btnPatreon.setMaximumSize(new Dimension(130, 26));
                                                                        btnPatreon.setPreferredSize(new Dimension(100, 26));
                                                                        btnPatreon.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnPatreonActionPerformed(evt);
                                                                            }
                                                                        });
                                                                        pnlAboutButtons.add(btnPatreon);

                                                                        pnlAbout.add(pnlAboutButtons);
                                                                        pnlAbout.add(glue2);

                                                                        pnlTabs.addTab("About", pnlAbout);

                                                                        pnlFooter.setPreferredSize(new Dimension(380, 36));
                                                                        pnlFooter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

                                                                        btnDone.setText("Done");
                                                                        btnDone.setMaximumSize(new Dimension(130, 26));
                                                                        btnDone.setMinimumSize(new Dimension(95, 23));
                                                                        btnDone.setName(""); // NOI18N
                                                                        btnDone.setPreferredSize(new Dimension(130, 26));
                                                                        btnDone.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnDoneActionPerformed(evt);
                                                                            }
                                                                        });
                                                                        pnlFooter.add(btnDone);

                                                                        btnCancel.setText("Cancel");
                                                                        btnCancel.setMaximumSize(new Dimension(130, 26));
                                                                        btnCancel.setMinimumSize(new Dimension(95, 23));
                                                                        btnCancel.setPreferredSize(new Dimension(130, 26));
                                                                        btnCancel.addActionListener(new java.awt.event.ActionListener()
                                                                        {
                                                                            public void actionPerformed(java.awt.event.ActionEvent evt)
                                                                            {
                                                                                btnCancelActionPerformed(evt);
                                                                            }
                                                                        });
                                                                        pnlFooter.add(btnCancel);

                                                                        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                                                                        getContentPane().setLayout(layout);
                                                                        layout.setHorizontalGroup(
                                                                            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGroup(layout.createSequentialGroup()
                                                                                .addContainerGap()
                                                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                    .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                                                    .addComponent(pnlTabs))
                                                                                .addContainerGap())
                                                                        );
                                                                        layout.setVerticalGroup(
                                                                            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                            .addGroup(layout.createSequentialGroup()
                                                                                .addContainerGap()
                                                                                .addComponent(pnlTabs)
                                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addContainerGap())
                                                                        );

                                                                        pack();
                                                                    }// </editor-fold>//GEN-END:initComponents

    private void btnDoneActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnDoneActionPerformed
    {//GEN-HEADEREND:event_btnDoneActionPerformed
        // done button
        try
        {
            Properties properties = Main.getInstance( ).getProperties( );
            String interactiveWindows = listData.toString( ).replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" );
            String interactiveWindowsBlacklist = blacklistData.toString( ).replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" );
            String[ ] windowArray = properties.getProperty( "WindowSize", "600x500" ).split( "x" );
            Dimension window = new Dimension( Integer.parseInt( windowArray[ 0 ] ), Integer.parseInt( windowArray[ 1 ] ) );
            float menuScaling = Float.parseFloat( properties.getProperty( "MenuDPI", "96" ) ) / 96;

            environmentReloadRequired = properties.getProperty( "Environment", "generic" ).equals( "virtual" ) != windowedMode ||
                                        !window.equals( windowSize ) ||
                                        !Color.decode( properties.getProperty( "Background", "#00FF00" ) ).equals( backgroundColour ) ||
                                        !properties.getProperty( "BackgroundMode", "centre" ).equals( backgroundMode ) ||
                                        !properties.getProperty( "BackgroundImage", "" ).equalsIgnoreCase( backgroundImage == null ? "" : backgroundImage );
            imageReloadRequired = !properties.getProperty( "Filter", "false" ).equalsIgnoreCase( filter ) || 
                                  Double.parseDouble( properties.getProperty( "Scaling", "1.0" ) ) != scaling || 
                                  Double.parseDouble( properties.getProperty( "Opacity", "1.0" ) ) != opacity;
            interactiveWindowReloadRequired = !properties.getProperty( "InteractiveWindows", "" ).equals( interactiveWindows ) || 
                                              !properties.getProperty( "InteractiveWindowsBlacklist", "" ).equals( interactiveWindowsBlacklist );
            FileOutputStream output = new FileOutputStream( configPath.toFile( ) );
            
            try
            {
                properties.setProperty( "AlwaysShowShimejiChooser", alwaysShowShimejiChooser.toString( ) );
                properties.setProperty( "AlwaysShowInformationScreen", alwaysShowInformationScreen.toString( ) );
                properties.setProperty( "Opacity", Double.toString( opacity ) );
                properties.setProperty( "Scaling", Double.toString( scaling ) );
                properties.setProperty( "Filter", filter );
                properties.setProperty( "InteractiveWindows", interactiveWindows );
                properties.setProperty( "InteractiveWindowsBlacklist", interactiveWindowsBlacklist );
                properties.setProperty( "Environment", windowedMode ? "virtual" : "generic" );
                if( windowedMode )
                {
                    properties.setProperty( "WindowSize", windowSize.width + "x" + windowSize.height );
                    properties.setProperty( "Background", String.format( "#%02X%02X%02X", backgroundColour.getRed( ), backgroundColour.getGreen( ), backgroundColour.getBlue( ) ) );
                    properties.setProperty( "BackgroundMode", backgroundMode );
                    properties.setProperty( "BackgroundImage", backgroundImage == null ? "" : backgroundImage );
                }
                
                properties.store( output, "Shimeji-ee Configuration Options" );
            }
            finally
            {
                output.close( );
            }
            
            FileOutputStream themeOutput = new FileOutputStream( themePath.toFile( ) );
            try
            {
                properties = new Properties( );
                properties.setProperty( "nimrodlf.p1", String.format( "#%02X%02X%02X", primaryColour1.getRed( ), primaryColour1.getGreen( ), primaryColour1.getBlue( ) ) );
                properties.setProperty( "nimrodlf.p2", String.format( "#%02X%02X%02X", primaryColour2.getRed( ), primaryColour2.getGreen( ), primaryColour2.getBlue( ) ) );
                properties.setProperty( "nimrodlf.p3", String.format( "#%02X%02X%02X", primaryColour3.getRed( ), primaryColour3.getGreen( ), primaryColour3.getBlue( ) ) );
                properties.setProperty( "nimrodlf.s1", String.format( "#%02X%02X%02X", secondaryColour1.getRed( ), secondaryColour1.getGreen( ), secondaryColour1.getBlue( ) ) );
                properties.setProperty( "nimrodlf.s2", String.format( "#%02X%02X%02X", secondaryColour2.getRed( ), secondaryColour2.getGreen( ), secondaryColour2.getBlue( ) ) );
                properties.setProperty( "nimrodlf.s3", String.format( "#%02X%02X%02X", secondaryColour3.getRed( ), secondaryColour3.getGreen( ), secondaryColour3.getBlue( ) ) );
                properties.setProperty( "nimrodlf.b", String.format( "#%02X%02X%02X", blackColour.getRed( ), blackColour.getGreen( ), blackColour.getBlue( ) ) );
                properties.setProperty( "nimrodlf.w", String.format( "#%02X%02X%02X", whiteColour.getRed( ), whiteColour.getGreen( ), whiteColour.getBlue( ) ) );
                properties.setProperty( "nimrodlf.menuOpacity", String.valueOf( (int)( menuOpacity * 255 ) ) );
                properties.setProperty( "nimrodlf.frameOpacity", "255" );
                properties.setProperty( "nimrodlf.font", String.format( "%s-%s-%d", 
                                                                        font.getName( ),
                                                                        font.getStyle( ) == Font.PLAIN ? "PLAIN" : 
                                                                        font.getStyle( ) == Font.BOLD ? "BOLD" :
                                                                        font.getStyle( ) == Font.ITALIC ? "ITALIC" :
                                                                        "BOLDITALIC",
                                                                        (int)( font.getSize( ) / menuScaling ) ) );
                properties.store( themeOutput, null );
            }
            finally
            {
                themeOutput.close( );
            }
        }
        catch( Exception e )
        {
        }
        dispose( );
    }//GEN-LAST:event_btnDoneActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCancelActionPerformed
    {//GEN-HEADEREND:event_btnCancelActionPerformed
        theme = oldTheme;
        refreshTheme( );
        dispose( );
    }//GEN-LAST:event_btnCancelActionPerformed

    private void btnAddInteractiveWindowActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnAddInteractiveWindowActionPerformed
    {//GEN-HEADEREND:event_btnAddInteractiveWindowActionPerformed
        // add button
        String inputValue = JOptionPane.showInputDialog( rootPane, Main.getInstance( ).getLanguageBundle( ).getProperty( "InteractiveWindowHintMessage" ), Main.getInstance( ).getLanguageBundle( ).getProperty( pnlInteractiveTabs.getSelectedIndex( ) == 0 ? "AddInteractiveWindow" : "BlacklistInteractiveWindow" ), JOptionPane.QUESTION_MESSAGE );
        if( inputValue != null && !inputValue.trim( ).isEmpty( ) && !inputValue.contains( "/" ) )
        {
            if( pnlInteractiveTabs.getSelectedIndex( ) == 0 )
            {
                listData.add( inputValue.trim( ) );
                lstInteractiveWindows.setListData( listData.toArray( ) );
            }
            else
            {
                blacklistData.add( inputValue.trim( ) );
                lstInteractiveWindowsBlacklist.setListData( blacklistData.toArray( ) );
            }
        }
    }//GEN-LAST:event_btnAddInteractiveWindowActionPerformed

    private void btnRemoveInteractiveWindowActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnRemoveInteractiveWindowActionPerformed
    {//GEN-HEADEREND:event_btnRemoveInteractiveWindowActionPerformed
        // delete button
        if( pnlInteractiveTabs.getSelectedIndex( ) == 0 )
        {
            if( lstInteractiveWindows.getSelectedIndex( ) != -1 )
            {
                listData.remove( lstInteractiveWindows.getSelectedIndex( ) );
                lstInteractiveWindows.setListData( listData.toArray( ) );
            }
        }
        else
        {
            if( lstInteractiveWindowsBlacklist.getSelectedIndex( ) != -1 )
            {
                blacklistData.remove( lstInteractiveWindowsBlacklist.getSelectedIndex( ) );
                lstInteractiveWindowsBlacklist.setListData( blacklistData.toArray( ) );
            }
        }
    }//GEN-LAST:event_btnRemoveInteractiveWindowActionPerformed

    private void chkAlwaysShowShimejiChooserItemStateChanged(ItemEvent evt)//GEN-FIRST:event_chkAlwaysShowShimejiChooserItemStateChanged
    {//GEN-HEADEREND:event_chkAlwaysShowShimejiChooserItemStateChanged
        alwaysShowShimejiChooser = evt.getStateChange( ) == ItemEvent.SELECTED;
    }//GEN-LAST:event_chkAlwaysShowShimejiChooserItemStateChanged

    private void radFilterItemStateChanged(ItemEvent evt)//GEN-FIRST:event_radFilterItemStateChanged
    {//GEN-HEADEREND:event_radFilterItemStateChanged
        if( evt.getStateChange( ) == ItemEvent.SELECTED )
        {
            Object source = evt.getItemSelectable( );
            
            if( source == radFilterNearest )
                filter = "nearest";
            else if( source == radFilterHqx )
                filter = "hqx";
            else
                filter = "bicubic";
        }
    }//GEN-LAST:event_radFilterItemStateChanged

    private void sldScalingStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_sldScalingStateChanged
    {//GEN-HEADEREND:event_sldScalingStateChanged
        if( !sldScaling.getValueIsAdjusting( ) )
        {
            if( sldScaling.getValue( ) == 0 )
                sldScaling.setValue( 5 );
            else
            {
                scaling = sldScaling.getValue( ) / 10.0;
                if( scaling == 2 || scaling == 3 || scaling == 4 || scaling == 6 || scaling == 8 )
                {
                    radFilterHqx.setEnabled( true );
                }
                else
                {
                    radFilterHqx.setEnabled( false );
                    if( filter.equals( "hqx" ) )
                    {
                        radFilterNearest.setSelected( true );
                    }
                }
            }
        }
    }//GEN-LAST:event_sldScalingStateChanged

    private void btnWebsiteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnWebsiteActionPerformed
    {//GEN-HEADEREND:event_btnWebsiteActionPerformed
        browseToUrl( "http://kilkakon.com/" );
    }//GEN-LAST:event_btnWebsiteActionPerformed

    private void btnDiscordActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnDiscordActionPerformed
    {//GEN-HEADEREND:event_btnDiscordActionPerformed
        browseToUrl( "https://discord.gg/NBq3zqfA2B" );
    }//GEN-LAST:event_btnDiscordActionPerformed

    private void btnPatreonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPatreonActionPerformed
    {//GEN-HEADEREND:event_btnPatreonActionPerformed
        browseToUrl( "https://patreon.com/kilkakon" );
    }//GEN-LAST:event_btnPatreonActionPerformed

    private void sldOpacityStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_sldOpacityStateChanged
    {//GEN-HEADEREND:event_sldOpacityStateChanged
        if( !sldOpacity.getValueIsAdjusting( ) )
        {
            if( sldOpacity.getValue( ) == 0 )
                sldOpacity.setValue( 5 );
            else
                opacity = sldOpacity.getValue( ) / 100.0;
        }
    }//GEN-LAST:event_sldOpacityStateChanged

    private void spnWindowHeightStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spnWindowHeightStateChanged
    {//GEN-HEADEREND:event_spnWindowHeightStateChanged
        windowSize.height = ( (SpinnerNumberModel)spnWindowHeight.getModel( ) ).getNumber( ).intValue( );
    }//GEN-LAST:event_spnWindowHeightStateChanged

    private void spnWindowWidthStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_spnWindowWidthStateChanged
    {//GEN-HEADEREND:event_spnWindowWidthStateChanged
        windowSize.width = ( (SpinnerNumberModel)spnWindowWidth.getModel( ) ).getNumber( ).intValue( );
    }//GEN-LAST:event_spnWindowWidthStateChanged

    private void btnBackgroundColourChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnBackgroundColourChangeActionPerformed
    {//GEN-HEADEREND:event_btnBackgroundColourChangeActionPerformed
        backgroundColour = chooseColour( backgroundColour, txtBackgroundColour, pnlBackgroundPreview, "ChooseBackgroundColour" );
    }//GEN-LAST:event_btnBackgroundColourChangeActionPerformed

    private void chkWindowModeEnabledItemStateChanged(ItemEvent evt)//GEN-FIRST:event_chkWindowModeEnabledItemStateChanged
    {//GEN-HEADEREND:event_chkWindowModeEnabledItemStateChanged
        windowedMode = evt.getStateChange( ) == ItemEvent.SELECTED;
        spnWindowWidth.setEnabled( windowedMode );
        spnWindowHeight.setEnabled( windowedMode );
        btnBackgroundColourChange.setEnabled( windowedMode );
        btnBackgroundImageChange.setEnabled( windowedMode );
        cmbBackgroundImageMode.setEnabled( windowedMode && backgroundImage != null );
        btnBackgroundImageRemove.setEnabled( windowedMode && backgroundImage != null );
    }//GEN-LAST:event_chkWindowModeEnabledItemStateChanged

    private void btnBackgroundImageChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnBackgroundImageChangeActionPerformed
    {//GEN-HEADEREND:event_btnBackgroundImageChangeActionPerformed
        final JFileChooser dialog = new JFileChooser( );
        dialog.setDialogTitle( Main.getInstance( ).getLanguageBundle( ).getProperty( "ChooseBackgroundImage" ) );
        //dialog.setFileFilter(  );
                
        if( dialog.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION )
        {
            try
            {
                backgroundImage = dialog.getSelectedFile( ).getCanonicalPath( );
                refreshBackgroundImage( );
            }
            catch( Exception e )
            {
                backgroundImage = null;
                lblBackgroundImage.setIcon( null );
            }
            cmbBackgroundImageMode.setEnabled( windowedMode && backgroundImage != null );
            btnBackgroundImageRemove.setEnabled( windowedMode && backgroundImage != null );
        }
    }//GEN-LAST:event_btnBackgroundImageChangeActionPerformed

    private void btnBackgroundImageRemoveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnBackgroundImageRemoveActionPerformed
    {//GEN-HEADEREND:event_btnBackgroundImageRemoveActionPerformed
        backgroundImage = null;
        lblBackgroundImage.setIcon( null );
        cmbBackgroundImageMode.setEnabled( false );
        btnBackgroundImageRemove.setEnabled( false );
    }//GEN-LAST:event_btnBackgroundImageRemoveActionPerformed

    private void cmbBackgroundImageModeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cmbBackgroundImageModeActionPerformed
    {//GEN-HEADEREND:event_cmbBackgroundImageModeActionPerformed
        if( cmbBackgroundImageMode.getSelectedIndex( ) > -1 )
            backgroundMode = backgroundModes[ cmbBackgroundImageMode.getSelectedIndex( ) ];
        refreshBackgroundImage( );
    }//GEN-LAST:event_cmbBackgroundImageModeActionPerformed

    private void chkAlwaysShowInformationScreenItemStateChanged(ItemEvent evt)//GEN-FIRST:event_chkAlwaysShowInformationScreenItemStateChanged
    {//GEN-HEADEREND:event_chkAlwaysShowInformationScreenItemStateChanged
        alwaysShowInformationScreen = evt.getStateChange( ) == ItemEvent.SELECTED;
    }//GEN-LAST:event_chkAlwaysShowInformationScreenItemStateChanged

    private void btnWhiteColourChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnWhiteColourChangeActionPerformed
    {//GEN-HEADEREND:event_btnWhiteColourChangeActionPerformed
        updateThemeHelpText( "WhiteColourHelpText" );
        whiteColour = chooseColour( whiteColour, txtWhiteColour, pnlWhiteColourPreview, "ChooseColour" );
        theme.setWhite( whiteColour );
        refreshTheme( );
    }//GEN-LAST:event_btnWhiteColourChangeActionPerformed

    private void btnBlackColourChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnBlackColourChangeActionPerformed
    {//GEN-HEADEREND:event_btnBlackColourChangeActionPerformed
        updateThemeHelpText( "BlackColourHelpText" );
        blackColour = chooseColour( blackColour, txtBlackColour, pnlBlackColourPreview, "ChooseColour" );
        theme.setBlack( blackColour );
        refreshTheme( );
    }//GEN-LAST:event_btnBlackColourChangeActionPerformed

    private void sldMenuOpacityStateChanged(javax.swing.event.ChangeEvent evt)//GEN-FIRST:event_sldMenuOpacityStateChanged
    {//GEN-HEADEREND:event_sldMenuOpacityStateChanged
        if( !sldMenuOpacity.getValueIsAdjusting( ) )
        {
            if( sldMenuOpacity.getValue( ) == 0 )
            sldMenuOpacity.setValue( 1 );
            else
            {
                menuOpacity = sldMenuOpacity.getValue( ) / 100.0;
                theme.setMenuOpacity( (int)( menuOpacity * 255 ) );
            }
        }
    }//GEN-LAST:event_sldMenuOpacityStateChanged

    private void btnSecondaryColour3ChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSecondaryColour3ChangeActionPerformed
    {//GEN-HEADEREND:event_btnSecondaryColour3ChangeActionPerformed
        updateThemeHelpText( "SecondaryColour3HelpText" );
        secondaryColour3 = chooseColour( secondaryColour3, txtSecondaryColour3, pnlSecondaryColour3Preview, "ChooseColour" );
        theme.setSecondary3( secondaryColour3 );
        refreshTheme( );
    }//GEN-LAST:event_btnSecondaryColour3ChangeActionPerformed

    private void txtSecondaryColour3FocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtSecondaryColour3FocusLost
    {//GEN-HEADEREND:event_txtSecondaryColour3FocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setSecondary3( secondaryColour3 );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtSecondaryColour3FocusLost

    private void btnSecondaryColour2ChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSecondaryColour2ChangeActionPerformed
    {//GEN-HEADEREND:event_btnSecondaryColour2ChangeActionPerformed
        updateThemeHelpText( "SecondaryColour2HelpText" );
        secondaryColour2 = chooseColour( secondaryColour2, txtSecondaryColour2, pnlSecondaryColour2Preview, "ChooseColour" );
        theme.setSecondary2( secondaryColour2 );
        refreshTheme( );
    }//GEN-LAST:event_btnSecondaryColour2ChangeActionPerformed

    private void txtSecondaryColour2FocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtSecondaryColour2FocusLost
    {//GEN-HEADEREND:event_txtSecondaryColour2FocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setSecondary2( secondaryColour2 );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtSecondaryColour2FocusLost

    private void btnSecondaryColour1ChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnSecondaryColour1ChangeActionPerformed
    {//GEN-HEADEREND:event_btnSecondaryColour1ChangeActionPerformed
        updateThemeHelpText( "SecondaryColour1HelpText" );
        secondaryColour1 = chooseColour( secondaryColour1, txtSecondaryColour1, pnlSecondaryColour1Preview, "ChooseColour" );
        theme.setSecondary1( secondaryColour1 );
        refreshTheme( );
    }//GEN-LAST:event_btnSecondaryColour1ChangeActionPerformed

    private void txtSecondaryColour1FocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtSecondaryColour1FocusLost
    {//GEN-HEADEREND:event_txtSecondaryColour1FocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setSecondary1( secondaryColour1 );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtSecondaryColour1FocusLost

    private void btnPrimaryColour2ChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPrimaryColour2ChangeActionPerformed
    {//GEN-HEADEREND:event_btnPrimaryColour2ChangeActionPerformed
        updateThemeHelpText( "PrimaryColour2HelpText" );
        primaryColour2 = chooseColour( primaryColour2, txtPrimaryColour2, pnlPrimaryColour2Preview, "ChooseColour" );
        theme.setPrimary2( primaryColour2 );
        refreshTheme( );
    }//GEN-LAST:event_btnPrimaryColour2ChangeActionPerformed

    private void txtPrimaryColour2FocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtPrimaryColour2FocusLost
    {//GEN-HEADEREND:event_txtPrimaryColour2FocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setPrimary2( primaryColour2 );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtPrimaryColour2FocusLost

    private void btnPrimaryColour1ChangeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnPrimaryColour1ChangeActionPerformed
    {//GEN-HEADEREND:event_btnPrimaryColour1ChangeActionPerformed
        updateThemeHelpText( "PrimaryColour1HelpText" );
        primaryColour1 = chooseColour( primaryColour1, txtPrimaryColour1, pnlPrimaryColour1Preview, "ChooseColour" );
        theme.setPrimary1( primaryColour1 );
        refreshTheme( );
    }//GEN-LAST:event_btnPrimaryColour1ChangeActionPerformed
    
    private void btnResetActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnResetActionPerformed
    {//GEN-HEADEREND:event_btnResetActionPerformed
        suppressTextChanged = true;
        float menuScaling = Float.parseFloat( Main.getInstance( ).getProperties( ).getProperty( "MenuDPI", "96" ) ) / 96;
        primaryColour1 = Color.decode( "#1EA6EB" );
        primaryColour2 = Color.decode( "#28B0F5" );
        primaryColour3 = Color.decode( "#32BAFF" );
        secondaryColour1 = Color.decode( "#BCBCBE" );
        secondaryColour2 = Color.decode( "#C6C6C8" );
        secondaryColour3 = Color.decode( "#D0D0D2" );
        blackColour = Color.decode( "#000000" );
        whiteColour = Color.decode( "#FFFFFF" );
        font = Font.decode( "SansSerif-PLAIN-12" );
        font = font.deriveFont( font.getSize( ) * menuScaling );
        pnlPrimaryColour1Preview.setBackground( primaryColour1 );
        txtPrimaryColour1.setText( String.format( "#%02X%02X%02X", primaryColour1.getRed( ), primaryColour1.getGreen( ), primaryColour1.getBlue( ) ) );
        pnlPrimaryColour2Preview.setBackground( primaryColour2 );
        txtPrimaryColour2.setText( String.format( "#%02X%02X%02X", primaryColour2.getRed( ), primaryColour2.getGreen( ), primaryColour2.getBlue( ) ) );
        pnlSecondaryColour1Preview.setBackground( secondaryColour1 );
        txtSecondaryColour1.setText( String.format( "#%02X%02X%02X", secondaryColour1.getRed( ), secondaryColour1.getGreen( ), secondaryColour1.getBlue( ) ) );
        pnlSecondaryColour2Preview.setBackground( secondaryColour2 );
        txtSecondaryColour2.setText( String.format( "#%02X%02X%02X", secondaryColour2.getRed( ), secondaryColour2.getGreen( ), secondaryColour2.getBlue( ) ) );
        pnlSecondaryColour3Preview.setBackground( secondaryColour3 );
        txtSecondaryColour3.setText( String.format( "#%02X%02X%02X", secondaryColour3.getRed( ), secondaryColour3.getGreen( ), secondaryColour3.getBlue( ) ) );
        pnlBlackColourPreview.setBackground( blackColour );
        txtBlackColour.setText( String.format( "#%02X%02X%02X", blackColour.getRed( ), blackColour.getGreen( ), blackColour.getBlue( ) ) );
        pnlWhiteColourPreview.setBackground( whiteColour );
        txtWhiteColour.setText( String.format( "#%02X%02X%02X", whiteColour.getRed( ), whiteColour.getGreen( ), whiteColour.getBlue( ) ) );
        menuOpacity = 1.0;
        theme.setPrimary1( primaryColour1 );
        theme.setPrimary2( primaryColour2 );
        theme.setPrimary3( primaryColour3 );
        theme.setSecondary1( secondaryColour1 );
        theme.setSecondary2( secondaryColour2 );
        theme.setSecondary3( secondaryColour3 );
        theme.setBlack( blackColour );
        theme.setWhite( whiteColour );
        theme.setFont( font );
        sldMenuOpacity.setValue( (int)( menuOpacity * 100 ) );
        refreshTheme( );
        pnlEditorPane.setText( "" );
        colourWasChanged = false;
        suppressTextChanged = false;
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnChangeFontActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnChangeFontActionPerformed
    {//GEN-HEADEREND:event_btnChangeFontActionPerformed
        JFrame frame = (JFrame)SwingUtilities.getWindowAncestor( this );
        NimRODFontDialog dialog = new NimRODFontDialog( frame, font );
        dialog.pack( );
        dialog.setLocationRelativeTo( frame );
        dialog.setVisible( true );
        font = dialog.getSelectedFont( );
        if( !dialog.isCanceled( ) )
        {
            theme.setFont( font );
            refreshTheme( );
        }
    }//GEN-LAST:event_btnChangeFontActionPerformed

    private void txtPrimaryColour1FocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtPrimaryColour1FocusLost
    {//GEN-HEADEREND:event_txtPrimaryColour1FocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setPrimary1( primaryColour1 );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtPrimaryColour1FocusLost

    private void txtBlackColourFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtBlackColourFocusLost
    {//GEN-HEADEREND:event_txtBlackColourFocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setBlack( blackColour );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtBlackColourFocusLost

    private void txtWhiteColourFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtWhiteColourFocusLost
    {//GEN-HEADEREND:event_txtWhiteColourFocusLost
        SwingUtilities.invokeLater( new Runnable( )
            {
                @Override
                public void run( )
                {
                    if( colourWasChanged )
                    {
                        colourWasChanged = false;
                        theme.setWhite( whiteColour );
                        refreshTheme( );
                    }
                }
            } );
    }//GEN-LAST:event_txtWhiteColourFocusLost

    private void txtPrimaryColour1FocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtPrimaryColour1FocusGained
    {//GEN-HEADEREND:event_txtPrimaryColour1FocusGained
        updateThemeHelpText( "PrimaryColour1HelpText" );
    }//GEN-LAST:event_txtPrimaryColour1FocusGained

    private void txtPrimaryColour2FocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtPrimaryColour2FocusGained
    {//GEN-HEADEREND:event_txtPrimaryColour2FocusGained
        updateThemeHelpText( "PrimaryColour2HelpText" );
    }//GEN-LAST:event_txtPrimaryColour2FocusGained

    private void txtSecondaryColour1FocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtSecondaryColour1FocusGained
    {//GEN-HEADEREND:event_txtSecondaryColour1FocusGained
        updateThemeHelpText( "SecondaryColour1HelpText" );
    }//GEN-LAST:event_txtSecondaryColour1FocusGained

    private void txtSecondaryColour2FocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtSecondaryColour2FocusGained
    {//GEN-HEADEREND:event_txtSecondaryColour2FocusGained
        updateThemeHelpText( "SecondaryColour2HelpText" );
    }//GEN-LAST:event_txtSecondaryColour2FocusGained

    private void txtSecondaryColour3FocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtSecondaryColour3FocusGained
    {//GEN-HEADEREND:event_txtSecondaryColour3FocusGained
        updateThemeHelpText( "SecondaryColour3HelpText" );
    }//GEN-LAST:event_txtSecondaryColour3FocusGained

    private void txtBlackColourFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtBlackColourFocusGained
    {//GEN-HEADEREND:event_txtBlackColourFocusGained
        updateThemeHelpText( "BlackColourHelpText" );
    }//GEN-LAST:event_txtBlackColourFocusGained

    private void txtWhiteColourFocusGained(java.awt.event.FocusEvent evt)//GEN-FIRST:event_txtWhiteColourFocusGained
    {//GEN-HEADEREND:event_txtWhiteColourFocusGained
        updateThemeHelpText( "WhiteColourHelpText" );
    }//GEN-LAST:event_txtWhiteColourFocusGained
    
    private void colourTextChanged( JTextField field )
    {
        if( suppressTextChanged || !isVisible( ) )
            return;
        
        String text = field.getText( );
        if( text == null )
            return;
        if( text.length( ) != 7 || !text.matches( "#[0-9a-fA-F]{6}" ) )
            return;
        
        Color newColour = Color.decode( text );
        
        if( field.equals( txtPrimaryColour1 ) )
        {
            if( !newColour.equals( primaryColour1 ) )
            {
                colourWasChanged = true;
                primaryColour1 = newColour;
                pnlPrimaryColour1Preview.setBackground( primaryColour1 );
            }
        }
        else if( field.equals( txtPrimaryColour2 ) )
        {
            if( !newColour.equals( primaryColour2 ) )
            {
                colourWasChanged = true;
                primaryColour2 = Color.decode( text );
                pnlPrimaryColour2Preview.setBackground( primaryColour2 );
            }
        }
        else if( field.equals( txtSecondaryColour1 ) )
        {
            if( !newColour.equals( secondaryColour1 ) )
            {
                colourWasChanged = true;
                secondaryColour1 = Color.decode( text );
                pnlSecondaryColour1Preview.setBackground( secondaryColour1 );
            }
        }
        else if( field.equals( txtSecondaryColour2 ) )
        {
            if( !newColour.equals( secondaryColour2 ) )
            {
                colourWasChanged = true;
                secondaryColour2 = Color.decode( text );
                pnlSecondaryColour2Preview.setBackground( secondaryColour2 );
            }
        }
        else if( field.equals( txtSecondaryColour3 ) )
        {
            if( !newColour.equals( secondaryColour3 ) )
            {
                colourWasChanged = true;
                secondaryColour3 = Color.decode( text );
                pnlSecondaryColour3Preview.setBackground( secondaryColour3 );
            }
        }
        else if( field.equals( txtBlackColour ) )
        {
            if( !newColour.equals( blackColour ) )
            {
                colourWasChanged = true;
                blackColour = Color.decode( text );
                pnlBlackColourPreview.setBackground( blackColour );
            }
        }
        else if( field.equals( txtWhiteColour ) )
        {
            if( !newColour.equals( whiteColour ) )
            {
                colourWasChanged = true;
                whiteColour = Color.decode( text );
                pnlWhiteColourPreview.setBackground( whiteColour );
            }
        }
        else if( field.equals( txtBackgroundColour ) )
        {
            if( !newColour.equals( backgroundColour ) )
            {
                backgroundColour = Color.decode( text );
                pnlBackgroundPreview.setBackground( backgroundColour );
            }
        }
    }
    
    private Color chooseColour( Color colour, JTextField field, JPanel preview, String title )
    {
        Color newColour = JColorChooser.showDialog( this, Main.getInstance( ).getLanguageBundle( ).getProperty( title ), colour );

        if( newColour != null )
        {
            suppressTextChanged = true;
            colour = newColour;
            field.setText( String.format( "#%02X%02X%02X", colour.getRed( ), colour.getGreen( ), colour.getBlue( ) ) );
            preview.setBackground( colour );
            suppressTextChanged = false;
        }
        
        return colour;
    }
    
    private void updateThemeHelpText( String languageEntry )
    {
        StringBuilder html = new StringBuilder( "<font style=\"font:" );
        if( font.getStyle( ) == Font.BOLD )
            html.append( "bold " );
        if( font.getStyle( ) == Font.ITALIC )
            html.append( "italic " );
        if( font.getStyle( ) == Font.BOLD + Font.ITALIC )
            html.append( "italic bold " );
        html.append( font.getSize( ) );
        html.append( "pt " );
        html.append( font.getFontName( ) );
        html.append( "; color:" );
        html.append( String.format( "#%02X%02X%02X", blackColour.getRed( ), blackColour.getGreen( ), blackColour.getBlue( ) ) );
        html.append( "\">" );
        html.append( Main.getInstance( ).getLanguageBundle( ).getProperty( languageEntry ) );
        html.append( "</font>" );
        
        pnlEditorPane.setText( html.toString( ) );
    }
    
    private void refreshBackgroundImage( )
    {
        Dimension size = pnlBackgroundImage.getPreferredSize( );
        Image image = new ImageIcon( backgroundImage ).getImage( );
        
        if( backgroundMode.equals( "stretch" ) )
        {
            image = image.getScaledInstance( size.width, 
                                             size.height,
                                             Image.SCALE_SMOOTH );
            
        }
        else if( !backgroundMode.equals( "centre" ) )
        {
            double factor = backgroundMode.equals( "fit" ) ?
                            Math.min( size.width / (double)image.getWidth( null ), size.height / (double)image.getHeight( null ) ) :
                            Math.max( size.width / (double)image.getWidth( null ), size.height / (double)image.getHeight( null ) );
            image = image.getScaledInstance( (int)( factor * image.getWidth( null ) ), 
                                             (int)( factor * image.getHeight( null ) ),
                                             Image.SCALE_SMOOTH );
        }
        
        lblBackgroundImage.setIcon( new ImageIcon( image ) );
        lblBackgroundImage.setPreferredSize( new Dimension( image.getWidth( null ), image.getHeight( null ) ) );
    }
    
    private void refreshTheme( )
    {
        try
        {
            NimRODLookAndFeel.setCurrentTheme( theme );
            UIManager.setLookAndFeel( lookAndFeel );
            SwingUtilities.updateComponentTreeUI( this );
            pack( );
        }
        catch( UnsupportedLookAndFeelException ex )
        {
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddInteractiveWindow;
    private javax.swing.JButton btnBackgroundColourChange;
    private javax.swing.JButton btnBackgroundImageChange;
    private javax.swing.JButton btnBackgroundImageRemove;
    private javax.swing.JButton btnBlackColourChange;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnChangeFont;
    private javax.swing.JButton btnDiscord;
    private javax.swing.JButton btnDone;
    private javax.swing.JButton btnPatreon;
    private javax.swing.JButton btnPrimaryColour1Change;
    private javax.swing.JButton btnPrimaryColour2Change;
    private javax.swing.JButton btnRemoveInteractiveWindow;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSecondaryColour1Change;
    private javax.swing.JButton btnSecondaryColour2Change;
    private javax.swing.JButton btnSecondaryColour3Change;
    private javax.swing.JButton btnWebsite;
    private javax.swing.JButton btnWhiteColourChange;
    private javax.swing.JCheckBox chkAlwaysShowInformationScreen;
    private javax.swing.JCheckBox chkAlwaysShowShimejiChooser;
    private javax.swing.JCheckBox chkWindowModeEnabled;
    private javax.swing.JComboBox<String> cmbBackgroundImageMode;
    private javax.swing.Box.Filler glue1;
    private javax.swing.Box.Filler glue2;
    private javax.swing.Box.Filler glueBackground;
    private javax.swing.Box.Filler glueBackground2;
    private javax.swing.Box.Filler glueBlackColoura;
    private javax.swing.Box.Filler glueBlackColourb;
    private javax.swing.Box.Filler gluePrimaryColour1a;
    private javax.swing.Box.Filler gluePrimaryColour1b;
    private javax.swing.Box.Filler gluePrimaryColour2a;
    private javax.swing.Box.Filler gluePrimaryColour2b;
    private javax.swing.Box.Filler glueSecondaryColour1a;
    private javax.swing.Box.Filler glueSecondaryColour1b;
    private javax.swing.Box.Filler glueSecondaryColour2a;
    private javax.swing.Box.Filler glueSecondaryColour2b;
    private javax.swing.Box.Filler glueSecondaryColour3a;
    private javax.swing.Box.Filler glueSecondaryColour3b;
    private javax.swing.Box.Filler glueWhiteColoura;
    private javax.swing.Box.Filler glueWhiteColourb;
    private javax.swing.ButtonGroup grpFilter;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea jTextArea1;
    private JLabel lblBackground;
    private JLabel lblBackgroundColour;
    private JLabel lblBackgroundImage;
    private JLabel lblBackgroundImageCaption;
    private JLabel lblBlackColour;
    private JLabel lblDevelopedBy;
    private JLabel lblDimensions;
    private JLabel lblDimensionsX;
    private JLabel lblFilter;
    private JLabel lblIcon;
    private JLabel lblKilkakon;
    private JLabel lblMenuOpacity;
    private JLabel lblOpacity;
    private JLabel lblPrimaryColour1;
    private JLabel lblPrimaryColour2;
    private JLabel lblScaling;
    private JLabel lblSecondaryColour1;
    private JLabel lblSecondaryColour2;
    private JLabel lblSecondaryColour3;
    private JLabel lblShimejiEE;
    private JLabel lblVersion;
    private JLabel lblWhiteColour;
    private javax.swing.JList lstInteractiveWindows;
    private javax.swing.JList lstInteractiveWindowsBlacklist;
    private JPanel pnlAbout;
    private JPanel pnlAboutButtons;
    private JPanel pnlBackgroundImage;
    private JPanel pnlBackgroundPreview;
    private JPanel pnlBackgroundPreviewContainer;
    private JPanel pnlBlackColourPreview;
    private JPanel pnlBlackColourPreviewContainer;
    private JPanel pnlBlacklistTab;
    private javax.swing.JEditorPane pnlEditorPane;
    private JPanel pnlFooter;
    private JPanel pnlGeneral;
    private JPanel pnlInteractiveButtons;
    private javax.swing.JTabbedPane pnlInteractiveTabs;
    private JPanel pnlInteractiveWindows;
    private JPanel pnlPrimaryColour1Preview;
    private JPanel pnlPrimaryColour1PreviewContainer;
    private JPanel pnlPrimaryColour2Preview;
    private JPanel pnlPrimaryColour2PreviewContainer;
    private javax.swing.JScrollPane pnlScrollPane;
    private JPanel pnlSecondaryColour1Preview;
    private JPanel pnlSecondaryColour1PreviewContainer;
    private JPanel pnlSecondaryColour2Preview;
    private JPanel pnlSecondaryColour2PreviewContainer;
    private JPanel pnlSecondaryColour3Preview;
    private JPanel pnlSecondaryColour3PreviewContainer;
    private javax.swing.JTabbedPane pnlTabs;
    private JPanel pnlTheme;
    private JPanel pnlThemeButtons;
    private JPanel pnlWhiteColourPreview;
    private JPanel pnlWhiteColourPreviewContainer;
    private JPanel pnlWhitelistTab;
    private JPanel pnlWindowMode;
    private javax.swing.JRadioButton radFilterBicubic;
    private javax.swing.JRadioButton radFilterHqx;
    private javax.swing.JRadioButton radFilterNearest;
    private javax.swing.Box.Filler rigid1;
    private javax.swing.Box.Filler rigid2;
    private javax.swing.Box.Filler rigid3;
    private javax.swing.Box.Filler rigid4;
    private javax.swing.JSlider sldMenuOpacity;
    private javax.swing.JSlider sldOpacity;
    private javax.swing.JSlider sldScaling;
    private javax.swing.JSpinner spnWindowHeight;
    private javax.swing.JSpinner spnWindowWidth;
    private JTextField txtBackgroundColour;
    private JTextField txtBlackColour;
    private JTextField txtPrimaryColour1;
    private JTextField txtPrimaryColour2;
    private JTextField txtSecondaryColour1;
    private JTextField txtSecondaryColour2;
    private JTextField txtSecondaryColour3;
    private JTextField txtWhiteColour;
    // End of variables declaration//GEN-END:variables
}
