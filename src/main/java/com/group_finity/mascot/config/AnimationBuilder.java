package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.wishes.constant.Constant;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ResourceBundle;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.image.ImagePairLoader.Filter;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.sound.SoundLoader;
/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class AnimationBuilder
{
    private static final Logger log = LoggerFactory.getLogger( AnimationBuilder.class.getName( ) );
    private final String condition;
    private String imageSet = "";
    private final List<Pose> poses = new ArrayList<Pose>( );
    private final List<Hotspot> hotspots = new ArrayList<Hotspot>( );
    private final ResourceBundle schema;
    private final String turn;

    public AnimationBuilder( final ResourceBundle schema, final Entry animationNode, final String imageSet ) throws ConfigurationException
    {
        if( !imageSet.equals( "" ) )
            this.imageSet = imageSet;

        this.schema = schema;
        this.condition = animationNode.getAttribute( schema.getString( "Condition" ) ) == null ? "true" : animationNode.getAttribute( schema.getString( "Condition" ) );
        this.turn = animationNode.getAttribute( schema.getString( "IsTurn" ) ) == null ? "false" : animationNode.getAttribute( schema.getString( "IsTurn" ) );


        for( final Entry frameNode : animationNode.selectChildren( schema.getString( "Pose" ) ) )
        {
            try
            {
                poses.add( loadPose( frameNode ) );
            }
            catch( IOException e )
            {
                throw new ConfigurationException( e.getMessage( ) );
            }
            catch( Exception e )
            {
                throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadPoseErrorMessage" ) + " " + frameNode.getAttributes( ).toString( ) );
            }
        }

        for( final Entry frameNode : animationNode.selectChildren( schema.getString( "Hotspot" ) ) )
        {
            try
            {
                hotspots.add( loadHotspot( frameNode ) );
            }
            catch( IOException e )
            {
                throw new ConfigurationException( e.getMessage( ) );
            }
            catch( Exception e )
            {
                throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadHotspotErrorMessage" ) + " " + frameNode.getAttributes( ).toString( ) );
            }
        }
        
    }

    private Pose loadPose( final Entry frameNode ) throws IOException
    {
        // 处理图片路径
        String imageAttr = frameNode.getAttribute( schema.getString( "Image" ) );
        String imageRightAttr = frameNode.getAttribute( schema.getString( "ImageRight" ) );
        
        // imageSet 可能已经是完整路径，也可能是相对路径
        Path imageText = null;
        Path imageRightText = null;
        
        if( imageAttr != null )
        {
            String fullPath = imageSet + imageAttr;
            File imageFile = new File( fullPath );
            if( imageFile.exists( ) )
            {
                imageText = imageFile.toPath( );
            }
            else
            {
                // 尝试在 img 目录下查找
                imageText = Paths.get( fullPath );
            }
        }
        
        if( imageRightAttr != null )
        {
            String fullPath = imageSet + imageRightAttr;
            File imageFile = new File( fullPath );
            if( imageFile.exists( ) )
            {
                imageRightText = imageFile.toPath( );
            }
            else
            {
                imageRightText = Paths.get( fullPath );
            }
        }
        final String anchorText = frameNode.getAttribute( schema.getString( "ImageAnchor" ) ) != null ? frameNode.getAttribute( schema.getString( "ImageAnchor" ) ) : null;
        final String moveText = frameNode.getAttribute( schema.getString( "Velocity" ) );
        final String durationText = frameNode.getAttribute( schema.getString( "Duration" ) );
        String soundText = frameNode.getAttribute( schema.getString( "Sound" ) ) != null ? frameNode.getAttribute( schema.getString( "Sound" ) ) : null;
        final String volumeText = frameNode.getAttribute( schema.getString( "Volume" ) ) != null ? frameNode.getAttribute( schema.getString( "Volume" ) ) : "0";

        final double opacity = Double.parseDouble( Main.getInstance( ).getProperties( ).getProperty( "Opacity", "1.0" ) );
        final double scaling = Double.parseDouble( Main.getInstance( ).getProperties( ).getProperty( "Scaling", "1.0" ) );
        
        String filterText = Main.getInstance( ).getProperties( ).getProperty( "Filter", "false" );
        Filter filter = Filter.NEAREST_NEIGHBOUR;
        if( filterText.equalsIgnoreCase( "true" ) || filterText.equalsIgnoreCase( "hqx" ) )
            filter = Filter.HQX;
        else if( filterText.equalsIgnoreCase( "bicubic" ) )
            filter = Filter.BICUBIC;

        if( imageText != null )
        {
            final String[] anchorCoordinates = anchorText.split( "," );
            final Point anchor = new Point( Integer.parseInt( anchorCoordinates[ 0 ] ), Integer.parseInt( anchorCoordinates[ 1 ] ) );

            try
            {
                ImagePairLoader.load( imageText, imageRightText, anchor, scaling, filter, opacity );
            }
            catch( Exception e )
            {
                String error = imageText.toString( );
                if( imageRightText != null )
                    error += ", " + imageRightText.toString( );
                throw new IOException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadImageErrorMessage" ) + " " + error );
            }
        }

        final String[] moveCoordinates = moveText.split( "," );
        int moveX = Integer.parseInt( moveCoordinates[ 0 ] );
        int moveY = Integer.parseInt( moveCoordinates[ 1 ] );
        moveX = Math.abs( moveX ) > 0 && Math.abs( moveX * scaling ) < 1 ? ( moveX > 0 ? 1 : -1 ) : (int)Math.round( moveX * scaling );
        moveY = Math.abs( moveY ) > 0 && Math.abs( moveY * scaling ) < 1 ? ( moveY > 0 ? 1 : -1 ) : (int)Math.round( moveY * scaling );
        final int duration = Integer.parseInt( durationText );

        if( soundText != null )
        {
            try
            {
                if( Paths.get( ".", "sound", soundText ).toFile( ).exists( ) )
                    soundText = Paths.get( ".", "sound", soundText ).toString( );
                else if( Paths.get( ".", "sound", imageSet, soundText ).toFile( ).exists( ) )
                    soundText = Paths.get( ".", "sound", imageSet, soundText ).toString( );
                else
                    soundText = Paths.get( ".", "img", imageSet, "sound", soundText ).toString( );

                SoundLoader.load( soundText, Float.parseFloat( volumeText ) );
                soundText += Float.parseFloat( volumeText );
            }
            catch( Exception e )
            {
                throw new IOException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadSoundErrorMessage" ) + soundText );
            }
        }

        final Pose pose = new Pose( imageText, imageRightText, moveX, moveY, duration, soundText != null ? soundText : null );


        return pose;
    }

    private Hotspot loadHotspot( final Entry frameNode ) throws IOException
    {
        final String shapeText = frameNode.getAttribute( schema.getString( "Shape" ) );
        final String originText = frameNode.getAttribute( schema.getString( "Origin" ) );
        final String sizeText = frameNode.getAttribute( schema.getString( "Size" ) );
        final String behaviourText = frameNode.getAttribute( schema.getString( "Behaviour" ) );
        final double scaling = Double.parseDouble( Main.getInstance( ).getProperties( ).getProperty( "Scaling", "1.0" ) );

        final String[ ] originCoordinates = originText.split( "," );
        final String[ ] sizeCoordinates = sizeText.split( "," );
        
        final Point origin = new Point( (int)Math.round( Integer.parseInt( originCoordinates[ 0 ] ) * scaling ),
                                        (int)Math.round( Integer.parseInt( originCoordinates[ 1 ] ) * scaling ) );
        final Dimension size = new Dimension( (int)Math.round( Integer.parseInt( sizeCoordinates[ 0 ] ) * scaling ), 
                                              (int)Math.round( Integer.parseInt( sizeCoordinates[ 1 ] ) * scaling ) );
        
        Shape shape;
        if( shapeText.equalsIgnoreCase( "Rectangle" ) )
        {
            shape = new Rectangle( origin, size );
        }
        else if( shapeText.equalsIgnoreCase( "Ellipse" ) )
        {
            shape = new Ellipse2D.Float( origin.x, origin.y, size.width, size.height );
        }
        else
        {
            throw new IOException( Main.getInstance( ).getLanguageBundle( ).getProperty( "HotspotShapeNotSupportedErrorMessage" ) + " " + shapeText );
        }

        final Hotspot hotspot = new Hotspot( behaviourText, shape );


        return hotspot;
    }

    public Animation buildAnimation( ) throws AnimationInstantiationException
    {
        try
        {
            return new Animation( Variable.parse( condition ), poses.toArray( new Pose[ 0 ] ), hotspots.toArray( new Hotspot[ 0 ] ), Boolean.parseBoolean( turn ) );
        }
        catch( final VariableException e )
        {
            throw new AnimationInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedConditionEvaluationErrorMessage" ), e );
        }
    }
}
