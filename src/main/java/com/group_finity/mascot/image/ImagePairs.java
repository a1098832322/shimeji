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

    /**
     * 按目录名片段清除指定 imageSet 的缓存，兼容开发与发行版的目录层级。
     */
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
            // Windows accepts both separators. Keep the original key for cache lookups.
            if( key.replace( '/', File.separatorChar ).contains( marker ) && imagepairs.remove( key ) != null )
            {
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
