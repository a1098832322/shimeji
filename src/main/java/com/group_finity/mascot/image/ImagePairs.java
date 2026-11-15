package com.group_finity.mascot.image;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class ImagePairs
{
    private static ConcurrentHashMap<String,ImagePair> imagepairs = new ConcurrentHashMap<String,ImagePair>( ); 

    public static void load( final String filename, final ImagePair imagepair )
    {
        if( !imagepairs.containsKey( filename ) )
            imagepairs.put( filename, imagepair );
    }

    public static ImagePair getImagePair( String filename )
    {
        if( !imagepairs.containsKey( filename ) )
            return null;
        ImagePair ip = imagepairs.get( filename );
        return ip;
    }	

    public static boolean contains( String filename )
    {
        return imagepairs.containsKey( filename );
    }

    public static void clear( )
    {
        imagepairs.clear( );
    }

    public static void removeAll( String searchTerm )
    {
        if( imagepairs.isEmpty( ) )
            return;

        for( Enumeration<String> key = imagepairs.keys( ); key.hasMoreElements( ); )
        {
            String filename = key.nextElement( );
            if( searchTerm.equals( Paths.get( filename ).getName( 2 ).toString( ) ) )
                imagepairs.remove( filename );
        }
    }

    /**
     * 清除属于指定 imageSet 的图片缓存，返回被清除的条目数。
     * <p>
     * 需求文档：docs/action-authoring-extensibility-requirements.md FR-04（D-3）。
     * 既有的 {@link #removeAll(String)} 依赖 {@code Paths.get(filename).getName(2)}，
     * 在 dev（{@code ./src/main/resources/img/<set>/...}）与
     * prod（{@code ./img/<set>/...}）两种布局下取到的层级不同，不可靠。
     * 本方法改为按「目录名片段」匹配，与 {@code AnimationBuilder.resolveImagePath}
     * 拼出的路径保持一致。
     */
    /**
     * 缓存快照，用于「重载失败时精确回滚」（FR-03 AC-03-5）。
     * 重载必须先清缓存再重建配置，否则 {@code ImagePairLoader.load} 会因路径命中而短路、
     * 新增/替换的 PNG 不会生效；一旦重建失败，用本快照把缓存恢复原状。
     */
    public static Map<String, ImagePair> snapshot( )
    {
        return new HashMap<String, ImagePair>( imagepairs );
    }

    public static void restore( final Map<String, ImagePair> snapshot )
    {
        imagepairs.clear( );
        if( snapshot != null )
        {
            imagepairs.putAll( snapshot );
        }
    }

    public static int clearForImageSet( final String imageSet )
    {
        if( imageSet == null || imageSet.trim( ).isEmpty( ) || imagepairs.isEmpty( ) )
        {
            return 0;
        }
        final java.nio.file.Path imageSetPath = Paths.get( imageSet );
        final String dirName = imageSetPath.getFileName( ) != null
                ? imageSetPath.getFileName( ).toString( )
                : imageSet;
        final String marker = File.separator + dirName + File.separator;
        int removed = 0;
        for( final String key : new ArrayList<String>( imagepairs.keySet( ) ) )
        {
            if( key.contains( marker ) )
            {
                imagepairs.remove( key );
                ++removed;
            }
        }
        return removed;
    }

    public static MascotImage getImage( String filename, boolean isLookRight )
    {
        if( !imagepairs.containsKey( filename ) )
            return null;
        return imagepairs.get( filename ).getImage( isLookRight );
    }
}