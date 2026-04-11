package com.group_finity.mascot.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Entry
{
    private Element element;

    private Map<String, String> attributes;

    private List<Entry> children;

    private Map<String, List<Entry>> selected = new HashMap<String, List<Entry>>( );

    public Entry( final Element element )
    {
        this.element = element;
    }

    public String getName( )
    {
        return this.element.getTagName( );
    }

    public String getText( )
    {
        return element.getTextContent( );
    }

    public Map<String, String> getAttributes( )
    {
        if( attributes != null )
        {
            return attributes;
        }

        attributes = new LinkedHashMap<String, String>( );
        final NamedNodeMap attrs = element.getAttributes( );
        for( int i = 0; i < attrs.getLength( ); ++i )
        {
            final Attr attr = (Attr)attrs.item( i );
            attributes.put( attr.getName( ), attr.getValue( ) );
        }

        return attributes;
    }

    public String getAttribute( final String attributeName )
    {
        final Attr attribute = element.getAttributeNode( attributeName );
        if( attribute == null )
        {
            return null;
        }
        return attribute.getValue( );
    }

//  public boolean hasAttribute( final String tagName )
//  {
//      return element.hasAttribute( tagName );
//  }

    public boolean hasChild( final String tagName )
    {
        for( final Entry child : getChildren( ) )
        {
            if ( child.getName( ).equals( tagName ) )
            {
                return true;
            }
        }
        return false;
    }

    public List<Entry> selectChildren( final String tagName )
    {
        List<Entry> children = selected.get( tagName );
        if( children != null )
        {
            return children;
        }
        children = new ArrayList<Entry>( );
        for( final Entry child : getChildren( ) )
        {
            if( child.getName( ).equals( tagName ) )
            {
                children.add( child );
            }
        }

        selected.put( tagName, children );

        return children;
    }

    public List<Entry> getChildren( )
    {
        if( children != null )
            return children;

        children = new ArrayList<Entry>( );
        
        final NodeList childNodes = element.getChildNodes( );
        for( int i = 0; i < childNodes.getLength( ); ++i )
        {
            final Node childNode = childNodes.item( i );
            if( childNode instanceof Element )
            {
                children.add( new Entry( (Element)childNode ) );
            }
        }

        return children;
    }
}
