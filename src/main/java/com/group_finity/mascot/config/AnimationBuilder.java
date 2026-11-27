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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import com.group_finity.mascot.script.VariableMap;
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
    /** FR-08：错误信息定位用的所属 Action 名；可为 null。 */
    private final String actionName;
    /** FR-08：当前 Pose 序号（从 1 开始）。 */
    private int poseIndex;
    /** FR-05：帧参数表达式的加载期求值上下文。 */
    private final VariableMap poseContext;

    public AnimationBuilder( final ResourceBundle schema, final Entry animationNode, final String imageSet ) throws ConfigurationException
    {
        this( schema, animationNode, imageSet, null, null );
    }

    /**
     * FR-05 / FR-06 / FR-08 扩展构造：带入所属 Action 名与参数，用于帧参数表达式求值
     * 及错误信息定位。原有三参构造保持兼容（此时错误信息只报 Pose 序号）。
     * <p>
     * 注意：Pose 是不可变对象，帧参数在<b>配置加载期</b>求值一次；上下文包含 Action 节点的
     * 全部属性，但不包含运行时的 {@code mascot}（加载期尚无角色实例）。
     *
     * @param actionName   所属 Action 名，仅用于诊断
     * @param actionParams Action 节点属性，作为表达式求值上下文
     */
    public AnimationBuilder( final ResourceBundle schema, final Entry animationNode, final String imageSet,
                             final String actionName, final Map<String, String> actionParams ) throws ConfigurationException
    {
        if( !imageSet.equals( "" ) )
            this.imageSet = imageSet;

        this.schema = schema;
        this.actionName = actionName;
        this.poseContext = createPoseContext( actionParams );
        this.condition = animationNode.getAttribute( schema.getString( "Condition" ) ) == null ? "true" : animationNode.getAttribute( schema.getString( "Condition" ) );
        this.turn = animationNode.getAttribute( schema.getString( "IsTurn" ) ) == null ? "false" : animationNode.getAttribute( schema.getString( "IsTurn" ) );

        for( final Entry frameNode : animationNode.selectChildren( schema.getString( "Pose" ) ) )
        {
            poseIndex++;
            try
            {
                poses.add( loadPose( frameNode ) );
            }
            catch( final ConfigurationException e )
            {
                throw e; // 保留带 Action / Pose 位置的精确错误，不再包一层
            }
            catch( final IOException e )
            {
                throw new ConfigurationException( e.getMessage( ), e );
            }
            catch( final Exception e )
            {
                throw new ConfigurationException( poseLoadError( frameNode, e ) );
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
                throw new ConfigurationException( poseLoadError( frameNode, e ) );
            }
        }
    }

    private static VariableMap createPoseContext( final Map<String, String> actionParams ) throws ConfigurationException
    {
        final VariableMap context = new VariableMap( );
        if( actionParams == null )
        {
            return context;
        }
        for( final Map.Entry<String, String> param : actionParams.entrySet( ) )
        {
            try
            {
                context.put( param.getKey( ), Variable.parse( param.getValue( ) ) );
            }
            catch( final Exception e )
            {
                throw new ConfigurationException( "Action parameter " + param.getKey( ) + "=\""
                        + param.getValue( ) + "\" cannot be parsed: " + e );
            }
        }
        return context;
    }

    private String describePose( )
    {
        return ( actionName == null ? "<Action>" : "Action[" + actionName + "]" )
                + " Pose[" + poseIndex + "]";
    }

    /** headless 单测下 languageBundle 可能为 null，给出可读兜底（FR-08 AC-08-1/5）。 */
    private String poseLoadError( final Entry poseNode, final Exception cause )
    {
        final Properties bundle = Main.getInstance( ).getLanguageBundle( );
        final String prefix = bundle != null ? bundle.getProperty( "FailedLoadPoseErrorMessage" ) : "Failed to load Pose";
        return prefix + " " + describePose( ) + " " + poseNode.getAttributes( ) + " caused by: " + cause;
    }

    /** FR-05：把整数字面量或 ${} / #{} 表达式求值为 int。 */
    private int evalInt( final String text, final String field ) throws ConfigurationException
    {
        if( text == null || text.trim( ).isEmpty( ) )
        {
            throw new ConfigurationException( describePose( ) + " " + field + " is empty" );
        }
        try
        {
            final Object value = Variable.parse( text.trim( ) ).get( poseContext );
            if( !( value instanceof Number ) )
            {
                throw new IllegalArgumentException( "result is not numeric: " + value );
            }
            return ( (Number) value ).intValue( );
        }
        catch( final Exception e )
        {
            throw new ConfigurationException( describePose( ) + " " + field + "=\"" + text
                    + "\" cannot be evaluated: " + e );
        }
    }

    /** FR-05：Velocity / ImageAnchor 的两个分量分别求值。 */
    private int[] evalPair( final String text, final String field ) throws ConfigurationException
    {
        final String[] parts = text == null ? new String[ 0 ] : text.split( ",", -1 );
        if( parts.length != 2 )
        {
            throw new ConfigurationException( describePose( ) + " " + field + "=\"" + text
                    + "\" must have exactly two comma-separated components" );
        }
        return new int[]{ evalInt( parts[ 0 ], field + "[x]" ), evalInt( parts[ 1 ], field + "[y]" ) };
    }

    /**
     * 把 Pose 的 {@code Image} 属性解析为实际文件路径。
     * <p>
     * 需求文档：docs/action-authoring-extensibility-requirements.md FR-06 / FR-08。
     * 抽成 public static 是为了让配置加载期的缺图预检复用同一套路径规则，
     * 避免「预检说存在、运行时说不存在」或反之。相对子目录（如 {@code sub/foo.png}）
     * 由 {@link java.nio.file.Path#resolve} 自然支持。
     */
    public static Path resolveImagePath( final String imageSet, final String imageAttr )
    {
        // FR-06：显式 "file:" 前缀表示绝对路径，用于外部素材包 / 临时预览图，
        // 无需把图片复制进每个皮肤目录。
        // 注意：历史配置大量使用 "/shime1.png" 表示「相对 imageSet 根」，
        // 所以不能用 Paths.get(...).isAbsolute() 判断 —— 那会破坏全部 132 个既有 Pose（AC-06-4）。
        if( imageAttr.startsWith( "file:" ) )
        {
            String rest = imageAttr.substring( "file:".length( ) );
            if( rest.startsWith( "//" ) )
            {
                rest = rest.substring( 2 ); // 兼容 file:///abs/path.png 写法
            }
            return Paths.get( rest );
        }

        String cleanPath = imageAttr;
        while( cleanPath.startsWith( "/" ) || cleanPath.startsWith( "\\" ) )
        {
            cleanPath = cleanPath.substring( 1 );
        }

        final Path basePath = Constant.isDevEnvironment
                ? Paths.get( ".", "src", "main", "resources", "img" )
                : Paths.get( ".", "img" );

        String imageSetName = imageSet;
        if( imageSet.contains( ":" ) || imageSet.startsWith( File.separator ) || imageSet.startsWith( "/" ) )
        {
            imageSetName = Paths.get( imageSet ).getFileName( ).toString( );
        }

        final String fullPath = basePath.resolve( imageSetName + File.separator + cleanPath ).toString( );
        final File imageFile = new File( fullPath );
        return imageFile.exists( ) ? imageFile.toPath( ) : Paths.get( fullPath );
    }

    private Pose loadPose( final Entry frameNode ) throws IOException, ConfigurationException
    {
        // 处理图片路径
        String imageAttr = frameNode.getAttribute( schema.getString( "Image" ) );
        String imageRightAttr = frameNode.getAttribute( schema.getString( "ImageRight" ) );
        
        // imageSet 可能已经是完整路径，也可能是相对路径
        Path imageText = null;
        Path imageRightText = null;
        
        if( imageAttr != null )
        {
            imageText = resolveImagePath( imageSet, imageAttr );
        }

        if( imageRightAttr != null )
        {
            imageRightText = resolveImagePath( imageSet, imageRightAttr );
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

        // FR-06 AC-06-2：缺图时给出完整解析路径 + Action / Pose 定位，而不是泛化错误
        if( imageText != null && !Files.isRegularFile( imageText ) )
        {
            throw new IOException( "Missing image file " + imageText.toAbsolutePath( )
                    + " (" + describePose( ) + " Image=\"" + imageAttr + "\")" );
        }
        if( imageRightText != null && !Files.isRegularFile( imageRightText ) )
        {
            throw new IOException( "Missing image file " + imageRightText.toAbsolutePath( )
                    + " (" + describePose( ) + " ImageRight=\"" + imageRightAttr + "\")" );
        }

        if( imageText != null )
        {
            final int[] anchorCoordinates = evalPair( anchorText, "ImageAnchor" );
            final Point anchor = new Point( anchorCoordinates[ 0 ], anchorCoordinates[ 1 ] );

            try
            {
                ImagePairLoader.load( imageText, imageRightText, anchor, scaling, filter, opacity );
            }
            catch( final Exception e )
            {
                final Properties bundle = Main.getInstance( ).getLanguageBundle( );
                final String fallback = "Failed to load image";
                final String prefix = bundle == null ? fallback
                        : bundle.getProperty( "FailedLoadImageErrorMessage", fallback );
                String paths = imageText.toAbsolutePath( ).toString( );
                if( imageRightText != null )
                    paths += ", " + imageRightText.toAbsolutePath( );
                throw new ConfigurationException( prefix + " " + paths + " (" + describePose( ) + ")", e );
            }
        }

        // FR-05：两个分量分别求值；求值结果仍参与下方既有的 scaling 换算，不绕过缩放
        final int[] moveCoordinates = evalPair( moveText, "Velocity" );
        int moveX = moveCoordinates[ 0 ];
        int moveY = moveCoordinates[ 1 ];
        moveX = Math.abs( moveX ) > 0 && Math.abs( moveX * scaling ) < 1 ? ( moveX > 0 ? 1 : -1 ) : (int)Math.round( moveX * scaling );
        moveY = Math.abs( moveY ) > 0 && Math.abs( moveY * scaling ) < 1 ? ( moveY > 0 ? 1 : -1 ) : (int)Math.round( moveY * scaling );

        final int duration = evalInt( durationText, "Duration" );
        // FR-05 风险条：Animation.getPoseAt() 会 time %= getDuration()，0/负数会除零
        if( duration < 1 )
        {
            throw new ConfigurationException( describePose( ) + " Duration must be >= 1 (was " + duration
                    + "); Animation.getPoseAt() would divide by zero" );
        }

        if( soundText != null )
        {
            try
            {
                // 根据开发/生产环境构建基础路径
                Path imgBasePath;
                if( Constant.isDevEnvironment )
                {
                    imgBasePath = Paths.get( ".", "src", "main", "resources", "img" );
                }
                else
                {
                    imgBasePath = Paths.get( ".", "img" );
                }
                
                // 如果 imageSet 是绝对路径，提取目录名
                String imageSetName = imageSet;
                if( imageSet.contains( ":" ) || imageSet.startsWith( File.separator ) || imageSet.startsWith( "/" ) )
                {
                    Path imageSetPath = Paths.get( imageSet );
                    imageSetName = imageSetPath.getFileName( ).toString( );
                }
                
                if( Paths.get( ".", "sound", soundText ).toFile( ).exists( ) )
                    soundText = Paths.get( ".", "sound", soundText ).toString( );
                else if( Paths.get( ".", "sound", imageSetName, soundText ).toFile( ).exists( ) )
                    soundText = Paths.get( ".", "sound", imageSetName, soundText ).toString( );
                else
                    soundText = imgBasePath.resolve( imageSetName + File.separator + "sound" + File.separator + soundText ).toString( );

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
