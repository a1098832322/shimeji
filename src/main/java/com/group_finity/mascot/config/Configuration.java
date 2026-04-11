package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import com.joconner.i18n.Utf8ResourceBundleControl;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class Configuration
{
    private static final Logger log = LoggerFactory.getLogger( Configuration.class.getName( ) );
    private final Map<String, String> constants = new LinkedHashMap<String, String>( 2 );
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<String, ActionBuilder>( );
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<String, BehaviorBuilder>( );
    private final Map<String, String> information = new LinkedHashMap<String, String>( 8 );
    private ResourceBundle schema;

    public void load( final Entry configurationNode, final String imageSet ) throws IOException, ConfigurationException
    {
        
        // prepare schema
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl( false );
        Locale locale;

        // check for Japanese XML tag and adapt locale accordingly
        if( configurationNode.hasChild( "\u52D5\u4F5C\u30EA\u30B9\u30C8" ) ||
            configurationNode.hasChild( "\u884C\u52D5\u30EA\u30B9\u30C8" ) )
        {
            locale = Locale.forLanguageTag( "ja-JP" );
        }
        else
        {
            locale = Locale.forLanguageTag( "en-US" );
        }
        
        schema = ResourceBundle.getBundle( "conf.schema", locale, utf8Control );
        
        for( Entry constant : configurationNode.selectChildren( schema.getString( "Constant" ) ) )
        {
            getConstants( ).put( constant.getAttribute( schema.getString( "Name" ) ),
                                 constant.getAttribute( schema.getString( "Value" ) ) );
        }

        for( final Entry list : configurationNode.selectChildren( schema.getString( "ActionList" ) ) )
        {

            for( final Entry node : list.selectChildren( schema.getString( "Action" ) ) )
            {
                final ActionBuilder action = new ActionBuilder( this, node, imageSet );

                if( getActionBuilders( ).containsKey( action.getName( ) ) )
                {
                    throw new ConfigurationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "DuplicateActionErrorMessage" ) + ": " + action.getName( ) );
                }

                getActionBuilders( ).put( action.getName( ), action );
            }
        }

        for( final Entry list : configurationNode.selectChildren( schema.getString( "BehaviourList" ) ) )
        {

            loadBehaviors( list, new ArrayList<String>( ) );
        }
        
        for( final Entry list : configurationNode.selectChildren( schema.getString( "Information" ) ) )
        {
            
            loadInformation( list );
        }

    }

	private void loadBehaviors( final Entry list, final List<String> conditions )
        {
            for( final Entry node : list.getChildren( ) )
            {
                if( node.getName( ).equals( schema.getString( "Condition" ) ) )
                {
                    final List<String> newConditions = new ArrayList<String>( conditions );
                    newConditions.add( node.getAttribute( schema.getString( "Condition" ) ) );

                    loadBehaviors(node, newConditions);
                }
                else if( node.getName( ).equals( schema.getString( "Behaviour" ) ) )
                {
                    final BehaviorBuilder behavior = new BehaviorBuilder( this, node, conditions );
                    this.getBehaviorBuilders( ).put( behavior.getName( ), behavior );
                }
            }
	}

	public Action buildAction(final String name, final Map<String, String> params) throws ActionInstantiationException {

		final ActionBuilder factory = this.actionBuilders.get(name);
		if (factory == null) {
			throw new ActionInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoCorrespondingActionFoundErrorMessage" ) + ": " + name);
		}

		return factory.buildAction( params );
	}
        
    private void loadInformation( final Entry list )
    {
        for( final Entry node : list.getChildren( ) )
        {
            if( node.getName( ).equals( schema.getString( "Name" ) ) ||
                node.getName( ).equals( schema.getString( "PreviewImage" ) ) ||
                node.getName( ).equals( schema.getString( "SplashImage" ) ) )
            {
                information.put( node.getName( ), node.getText( ) );
            }
            else if( node.getName( ).equals( schema.getString( "Artist" ) ) ||
                     node.getName( ).equals( schema.getString( "Scripter" ) ) || 
                     node.getName( ).equals( schema.getString( "Commissioner" ) ) ||
                     node.getName( ).equals( schema.getString( "Support" ) ) )
            {
                String nameText = node.getAttribute( schema.getString( "Name" ) ) != null ? node.getAttribute( schema.getString( "Name" ) ) : null;
                String linkText = node.getAttribute( schema.getString( "URL" ) ) != null ? node.getAttribute( schema.getString( "URL" ) ) : null;
                
                if( nameText != null )
                {
                    information.put( node.getName( ) + schema.getString( "Name" ), nameText );
                    if( linkText != null )
                        information.put( node.getName( ) + schema.getString( "URL" ), linkText );
                }
            }
        }
    }

	public void validate() throws ConfigurationException{

		for(final ActionBuilder builder : getActionBuilders().values()) {
			builder.validate();
		}
		for(final BehaviorBuilder builder : getBehaviorBuilders().values()) {
			builder.validate();
		}
	}

    public Behavior buildNextBehavior( final String previousName, final Mascot mascot ) throws BehaviorInstantiationException
    {
        final VariableMap context = new VariableMap( );
        context.putAll( getConstants( ) ); // put first so they can't override mascot
        context.put( "mascot", mascot );

        final List<BehaviorBuilder> candidates = new ArrayList<BehaviorBuilder>( );
        long totalFrequency = 0;
        for( final BehaviorBuilder behaviorFactory : this.getBehaviorBuilders( ).values( ) )
        {
            try
            {
                if( behaviorFactory.isEffective( context ) && isBehaviorEnabled( behaviorFactory, mascot ) )
                {
                    candidates.add( behaviorFactory );
                    totalFrequency += behaviorFactory.getFrequency( );
                }
            }
            catch( final VariableException e )
            {
            }
        }

        if( previousName != null )
        {
            final BehaviorBuilder previousBehaviorFactory = this.getBehaviorBuilders( ).get( previousName );
            if( !previousBehaviorFactory.isNextAdditive( ) )
            {
                totalFrequency = 0;
                candidates.clear( );
            }
            for( final BehaviorBuilder behaviorFactory : previousBehaviorFactory.getNextBehaviorBuilders( ) )
            {
                try
                {
                    if( behaviorFactory.isEffective( context ) && isBehaviorEnabled( behaviorFactory, mascot ) )
                    {
                        candidates.add( behaviorFactory );
                        totalFrequency += behaviorFactory.getFrequency( );
                    }
                }
                catch( final VariableException e )
                {
                }
            }
        }

        if( totalFrequency == 0 )
        {
            if( Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Multiscreen", "true" ) ) )
            {
                mascot.setAnchor( new Point( (int)( Math.random( ) * ( mascot.getEnvironment( ).getScreen( ).getRight( ) - mascot.getEnvironment( ).getScreen( ).getLeft( ) ) ) + mascot.getEnvironment( ).getScreen( ).getLeft( ),
                                             mascot.getEnvironment( ).getScreen( ).getTop( ) - 256 ) );
            }
            else
            {
                mascot.setAnchor( new Point( (int)( Math.random( ) * ( mascot.getEnvironment( ).getWorkArea( ).getRight( ) - mascot.getEnvironment( ).getWorkArea( ).getLeft( ) ) ) + mascot.getEnvironment( ).getWorkArea( ).getLeft( ),
                                             mascot.getEnvironment( ).getWorkArea( ).getTop( ) - 256 ) );
            }
            return buildBehavior( schema.getString( UserBehavior.BEHAVIOURNAME_FALL ) );
        }

        double random = Math.random( ) * totalFrequency;

        for( final BehaviorBuilder behaviorFactory : candidates )
        {
            random -= behaviorFactory.getFrequency( );
            if( random < 0 )
            {
                return behaviorFactory.buildBehavior( );
            }
        }

        return null;
    }

    public Behavior buildBehavior( final String name, final Mascot mascot ) throws BehaviorInstantiationException
    {
        if( behaviorBuilders.containsKey( name ) )
        {
            if( isBehaviorEnabled( name, mascot ) )
            {
                return getBehaviorBuilders( ).get( name ).buildBehavior( );
            }
            else
            {
                if( Boolean.parseBoolean( Main.getInstance( ).getProperties( ).getProperty( "Multiscreen", "true" ) ) )
                {
                    mascot.setAnchor( new Point( (int)( Math.random( ) * ( mascot.getEnvironment( ).getScreen( ).getRight( ) - mascot.getEnvironment( ).getScreen( ).getLeft( ) ) ) + mascot.getEnvironment( ).getScreen( ).getLeft( ),
                                                 mascot.getEnvironment( ).getScreen( ).getTop( ) - 256 ) );
                }
                else
                {
                    mascot.setAnchor( new Point( (int)( Math.random( ) * ( mascot.getEnvironment( ).getWorkArea( ).getRight( ) - mascot.getEnvironment( ).getWorkArea( ).getLeft( ) ) ) + mascot.getEnvironment( ).getWorkArea( ).getLeft( ),
                                                 mascot.getEnvironment( ).getWorkArea( ).getTop( ) - 256 ) );
                }
                return buildBehavior( schema.getString( UserBehavior.BEHAVIOURNAME_FALL ) );
            }
        }
        else
            throw new BehaviorInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoBehaviourFoundErrorMessage" ) + " (" + name + ")" );
    }

    public Behavior buildBehavior( final String name ) throws BehaviorInstantiationException
    {
        if( behaviorBuilders.containsKey( name ) )
            return getBehaviorBuilders( ).get( name ).buildBehavior( );
        else
            throw new BehaviorInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "NoBehaviourFoundErrorMessage" ) + " (" + name + ")" );
    }
    
    public boolean isBehaviorEnabled( final BehaviorBuilder builder, final Mascot mascot )
    {
        if( builder.isToggleable( ) )
        {
            for( String behaviour : Main.getInstance( ).getProperties( ).getProperty( "DisabledBehaviours." + mascot.getImageSet( ), "" ).split( "/" ) )
            {
                if( behaviour.equals( builder.getName( ) ) )
                    return false;
            }
        }
        return true;
    }
    
    public boolean isBehaviorEnabled( final String name, final Mascot mascot )
    {
        if( behaviorBuilders.containsKey( name ) )
            return isBehaviorEnabled( getBehaviorBuilders( ).get( name ), mascot );
        else
            return false;
    }
    
    public boolean isBehaviorHidden( final String name )
    {
        if( behaviorBuilders.containsKey( name ) )
            return getBehaviorBuilders( ).get( name ).isHidden( );
        else
            return false;
    }
    
    public boolean isBehaviorToggleable( final String name )
    {
        if( behaviorBuilders.containsKey( name ) )
            return getBehaviorBuilders( ).get( name ).isToggleable( );
        else
            return false;
    }
    
    private Map<String, String> getConstants( )
    {
        return constants;
    }

    Map<String, ActionBuilder> getActionBuilders( )
    {
        return actionBuilders;
    }

    private Map<String, BehaviorBuilder> getBehaviorBuilders( )
    {
        return behaviorBuilders;
    }

    public java.util.Set<String> getBehaviorNames( )
    {
        return behaviorBuilders.keySet( );
    }

    public boolean containsInformationKey( String key )
    {
        return information.containsKey( key );
    }

    public String getInformation( String key )
    {
        return information.get( key );
    }
        
    public ResourceBundle getSchema( )
    {
        return schema;
    }
}
