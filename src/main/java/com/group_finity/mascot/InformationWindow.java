/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.group_finity.mascot;

import com.group_finity.mascot.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Color;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;

/**
 * @author Kilkakon
 */
public class InformationWindow extends javax.swing.JFrame
{
    private final Path themePath = Paths.get( ".", "conf", "theme.properties" );
    
    /**
     * Creates new form InformationWindow
     */
    public InformationWindow( )
    {
        initComponents( );
    }
    
    public void init( final String imageSet, final Configuration config )
    {
        // initialise controls
        setLocationRelativeTo( null );
        
        // load icon
        Image icon = null;
        try
        {
            icon = new ImageIcon( Paths.get( ".", "img", "icon.png" ).toString( ) ).getImage( );
        }
        catch( final Exception e )
        {
            // not bothering reporting errors with loading the tray icon as it would have already been reported to the user by now
        }
        finally
        {
            if( icon == null )
                icon = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_RGB );
        }
        setIconImage( icon );
        
        // load image
        Image image = new ImageIcon( Paths.get( ".", "img", imageSet, config.getInformation( "SplashImage" ) ).toString( ) ).getImage( );
        try
        {
            lblSplashImage.setIcon( new ImageIcon( image ) );
        }
        catch( Exception e )
        {
        }
        
        // text
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
        Color textColour = Color.decode( themeProperties.getProperty( "nimrodlf.b", "#000000" ) );
        Color linkColour = Color.decode( themeProperties.getProperty( "nimrodlf.p2", "#28B0F5" ) );
        
        final Properties language = Main.getInstance( ).getLanguageBundle( );
        setTitle( config.containsInformationKey( "Name" ) ? config.getInformation( "Name" ) : language.getProperty( "Information" ) );
        
        StringBuilder html = new StringBuilder( "<center style=\"font:" );
        if( lblSplashImage.getFont( ).getStyle( ) == Font.BOLD )
            html.append( "bold " );
        if( lblSplashImage.getFont( ).getStyle( ) == Font.ITALIC )
            html.append( "italic " );
        if( lblSplashImage.getFont( ).getStyle( ) == Font.BOLD + Font.ITALIC )
            html.append( "italic bold " );
        html.append( lblSplashImage.getFont( ).getSize( ) );
        html.append( "pt " );
        html.append( lblSplashImage.getFont( ).getFontName( ) );
        html.append( "; color:" );
        html.append( String.format( "#%02X%02X%02X", textColour.getRed( ), textColour.getGreen( ), textColour.getBlue( ) ) );
        html.append( "\">" );
        if( config.containsInformationKey( "ArtistName" ) )
        {
            html.append( language.getProperty( "ArtBy" ) );
            html.append( " " );
            if( config.containsInformationKey( "ArtistURL" ) )
            {
                html.append( "<a href=\"" );
                html.append( config.getInformation( "ArtistURL" ) );
                html.append( "\" style=\"color:" );
                html.append( String.format( "#%02X%02X%02X", linkColour.getRed( ), linkColour.getGreen( ), linkColour.getBlue( ) ) );
                html.append( "\">" );
            }
            html.append( config.getInformation( "ArtistName" ) );
            if( config.containsInformationKey( "ArtistURL" ) )
                html.append( "</a>" );
        }
        if( config.containsInformationKey( "ScripterName" ) )
        {
            if( config.containsInformationKey( "ArtistName" ) )
                html.append( " - " );
            html.append( language.getProperty( "ScriptedBy" ) );
            html.append( " " );
            if( config.containsInformationKey( "ScripterURL" ) )
            {
                html.append( "<a href=\"" );
                html.append( config.getInformation( "ScripterURL" ) );
                html.append( "\" style=\"color:" );
                html.append( String.format( "#%02X%02X%02X", linkColour.getRed( ), linkColour.getGreen( ), linkColour.getBlue( ) ) );
                html.append( "\">" );
            }
            html.append( config.getInformation( "ScripterName" ) );
            if( config.containsInformationKey( "ScripterURL" ) )
                html.append( "</a>" );
        }
        if( config.containsInformationKey( "CommissionerName" ) )
        {
            if( config.containsInformationKey( "ArtistName" ) || config.containsInformationKey( "ScripterName" ) )
                html.append( " - " );
            html.append( language.getProperty( "CommissionedBy" ) );
            html.append( " " );
            if( config.containsInformationKey( "CommissionerURL" ) )
            {
                html.append( "<a href=\"" );
                html.append( config.getInformation( "CommissionerURL" ) );
                html.append( "\" style=\"color:" );
                html.append( String.format( "#%02X%02X%02X", linkColour.getRed( ), linkColour.getGreen( ), linkColour.getBlue( ) ) );
                html.append( "\">" );
            }
            html.append( config.getInformation( "CommissionerName" ) );
            if( config.containsInformationKey( "CommissionerURL" ) )
                html.append( "</a>" );
        }
        if( config.containsInformationKey( "SupportName" ) )
        {
            if( config.containsInformationKey( "ArtistName" ) || config.containsInformationKey( "ScripterName" ) || config.containsInformationKey( "CommissionerName" ) )
                html.append( " - " );
            html.append( language.getProperty( "SupportAt" ) );
            html.append( " " );
            if( config.containsInformationKey( "SupportURL" ) )
            {
                html.append( "<a href=\"" );
                html.append( config.getInformation( "SupportURL" ) );
                html.append( "\" style=\"color:" );
                html.append( String.format( "#%02X%02X%02X", linkColour.getRed( ), linkColour.getGreen( ), linkColour.getBlue( ) ) );
                html.append( "\">" );
            }
            html.append( config.getInformation( "SupportName" ) );
            if( config.containsInformationKey( "SupportURL" ) )
                html.append( "</a>" );
        }
        html.append( "</center>" );
        
        pnlEditorPane.setText( html.toString( ) );
        pnlEditorPane.addHyperlinkListener( new HyperlinkListener( )
        {
            @Override
            public void hyperlinkUpdate( HyperlinkEvent e )
            {
                if( e.getEventType( ) == HyperlinkEvent.EventType.ACTIVATED )
                {
                    StringTokenizer st = new StringTokenizer( e.getDescription( ), " " );
                    if( st.hasMoreTokens( ) )
                    {
                        String url = st.nextToken( );
                        if( JOptionPane.showConfirmDialog(
                            InformationWindow.this,
                            language.getProperty( "ConfirmVisitWebsiteMessage" ) + "\n" + language.getProperty( "ExerciseCautionAndBewareSusLinksMessage" ) + "\n" +url,
                            language.getProperty( "VisitWebsite" ),
                            JOptionPane.YES_NO_OPTION ) == JOptionPane.YES_OPTION )
                        {
                            try
                            {
                                Desktop desktop = Desktop.isDesktopSupported( ) ? Desktop.getDesktop( ) : null;
                                if( desktop != null && desktop.isSupported( Desktop.Action.BROWSE ) )
                                    desktop.browse( new URI( url ) );
                                else
                                    throw new UnsupportedOperationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedOpenWebBrowserErrorMessage" ) + " " + url );
                            }
                            catch( Exception exc )
                            {
                                JOptionPane.showMessageDialog( InformationWindow.this, exc.toString( ), "Error", JOptionPane.PLAIN_MESSAGE );
                            }
                        }
                    }
                }
            }
        } );
        btnClose.setText( language.getProperty( "Close" ) );
    }
    
    public boolean display( )
    {
        float menuScaling = Float.parseFloat( Main.getInstance( ).getProperties( ).getProperty( "MenuDPI", "96" ) ) / 96;
        pnlEditorPane.setBackground( getBackground( ) );
        pnlEditorPane.setBorder( null );
        pnlScrollPane.setBorder( null );
        pnlScrollPane.setViewportBorder( null );
        
        // scale controls to fit
        lblSplashImage.setPreferredSize( new Dimension( (int)( lblSplashImage.getIcon( ).getIconWidth( ) * menuScaling ), (int)( lblSplashImage.getIcon( ).getIconHeight( ) * menuScaling ) ) );
        pnlEditorPane.setPreferredSize( new Dimension( (int)( pnlEditorPane.getPreferredSize( ).width * menuScaling ), (int)( pnlEditorPane.getPreferredSize( ).height * menuScaling ) ) );
        pnlScrollPane.setPreferredSize( new Dimension( (int)( pnlScrollPane.getPreferredSize( ).width * menuScaling ), (int)( pnlScrollPane.getPreferredSize( ).height * menuScaling ) ) );
        btnClose.setPreferredSize( new Dimension( (int)( btnClose.getPreferredSize( ).width * menuScaling ), (int)( btnClose.getPreferredSize( ).height * menuScaling ) ) );
        pnlFooter.setPreferredSize( new Dimension( pnlFooter.getPreferredSize( ).width, btnClose.getPreferredSize( ).height + 6 ) );
        pack( );
        setLocationRelativeTo( null );
        setVisible( true );
        
        return true;
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
        java.awt.GridBagConstraints gridBagConstraints;

        pnlImage = new javax.swing.JPanel();
        lblSplashImage = new javax.swing.JLabel();
        pnlScrollPane = new javax.swing.JScrollPane();
        pnlEditorPane = new javax.swing.JEditorPane();
        pnlFooter = new javax.swing.JPanel();
        btnClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        pnlImage.setLayout(new javax.swing.BoxLayout(pnlImage, javax.swing.BoxLayout.Y_AXIS));

        lblSplashImage.setAlignmentX(0.5F);
        pnlImage.add(lblSplashImage);

        pnlEditorPane.setEditable(false);
        pnlEditorPane.setBorder(null);
        pnlEditorPane.setContentType("text/html"); // NOI18N
        pnlEditorPane.setText("");
        pnlScrollPane.setViewportView(pnlEditorPane);

        pnlFooter.setPreferredSize(new Dimension(380, 36));
        pnlFooter.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 5));

        btnClose.setText("Close");
        btnClose.setMaximumSize(new Dimension(130, 26));
        btnClose.setMinimumSize(new Dimension(95, 23));
        btnClose.setName(""); // NOI18N
        btnClose.setPreferredSize(new Dimension(130, 26));
        btnClose.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnCloseActionPerformed(evt);
            }
        });
        pnlFooter.add(btnClose);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlImage, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                    .addComponent(pnlFooter, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlScrollPane, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlImage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFooter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCloseActionPerformed
    {//GEN-HEADEREND:event_btnCloseActionPerformed
        dispose( );
    }//GEN-LAST:event_btnCloseActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main( String args[] )
    {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try
        {
            for( javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels() )
            {
                if( "Nimbus".equals( info.getName() ) )
                {
                    javax.swing.UIManager.setLookAndFeel( info.getClassName() );
                    break;
                }
            }
        }
        catch( ClassNotFoundException ex )
        {
            LoggerFactory.getLogger( InformationWindow.class ).error("Error", ex);
        }
        catch( InstantiationException ex )
        {
            LoggerFactory.getLogger( InformationWindow.class ).error("Error", ex);
        }
        catch( IllegalAccessException ex )
        {
            LoggerFactory.getLogger( InformationWindow.class ).error("Error", ex);
        }
        catch( javax.swing.UnsupportedLookAndFeelException ex )
        {
            LoggerFactory.getLogger( InformationWindow.class ).error("Error", ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
            }
        } );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClose;
    private javax.swing.JLabel lblSplashImage;
    private javax.swing.JEditorPane pnlEditorPane;
    private javax.swing.JPanel pnlFooter;
    private javax.swing.JPanel pnlImage;
    private javax.swing.JScrollPane pnlScrollPane;
    // End of variables declaration//GEN-END:variables
}
