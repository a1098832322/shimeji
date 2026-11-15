package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.glados.GladosManagedConstraints;
import com.group_finity.mascot.glados.GladosOptions;
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
    /** 每个 Action 的来源文件，用于区分「跨文件覆盖」与「单文件内重复」（FR-02）。 */
    private final Map<String, String> actionSources = new LinkedHashMap<String, String>( );
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<String, BehaviorBuilder>( );
    private final Map<String, String> information = new LinkedHashMap<String, String>( 8 );
    private ResourceBundle schema;

    public void load( final Entry configurationNode, final String imageSet ) throws IOException, ConfigurationException
    {
        load( configurationNode, imageSet, "<unnamed>" );
    }

    /**
     * 加载一份动作/行为定义。
     * <p>
     * 需求文档：docs/action-authoring-extensibility-requirements.md FR-02
     * <p>
     * {@code source} 仅用于诊断与冲突判定：同名 Action 来自<b>不同</b>来源文件时按
     * 「后写覆盖前写」处理（与 Behavior 的 put 语义对齐），来自<b>同一</b>文件时仍视为
     * 笔误并抛异常。这样既支持「官方基础包 + 用户覆盖包」，又不会掩盖单文件内的重复声明。
     *
     * @param source 本份定义的文件路径，建议使用绝对路径
     */
    public void load( final Entry configurationNode, final String imageSet, final String source ) throws IOException, ConfigurationException
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

                final String previousSource = getActionSources( ).get( action.getName( ) );
                if( previousSource != null )
                {
                    if( previousSource.equals( source ) )
                    {
                        // headless 单测下 languageBundle 可能未初始化，给出可读兜底（FR-08 AC-08-1）
                        final java.util.Properties bundle = Main.getInstance( ).getLanguageBundle( );
                        final String prefix = bundle != null
                                ? bundle.getProperty( "DuplicateActionErrorMessage" )
                                : "Duplicate action";
                        throw new ConfigurationException( prefix + ": " + action.getName( ) + " (" + source + ")" );
                    }
                    log.warn( "Action '{}' from {} overrides the earlier definition from {}",
                            action.getName( ), source, previousSource );
                }

                getActionSources( ).put( action.getName( ), source );
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

    /**
     * 合并 GLaDOS 行为表（glados-behaviors.xml）到本配置。
     * <p>
     * 设计文档：docs/glados-ai-integration-design.md §5、
     * docs/glados-managed-constraints-design.md §4.2
     * <p>
     * 行为复用本配置中已加载的动作（不新增动作），仅补充 BehaviorList。
     * <p>
     * <b>同名覆盖语义：</b>{@code loadBehaviors} 使用 {@code put(name, builder)}，
     * 后写覆盖前写。本方法在 {@code Main.loadConfiguration()}（已加载 actions.xml +
     * behaviors.xml）之后调用，因此表中的同名声明会顶掉自主行为树里的原始声明。
     * <p>
     * 托管模式禁用 {@code SplitIntoTwo} / {@code PullUpShimeji}（Frequency=0）
     * 正是依赖这一覆盖语义，切勿改成 {@code putIfAbsent} 之类的「保留已有」实现。
     *
     * @param gladosNode glados-behaviors.xml 的根节点
     */
    public void loadGladosBehaviors( final Entry gladosNode )
    {
        for( final Entry list : gladosNode.selectChildren( schema.getString( "BehaviourList" ) ) )
        {
            loadBehaviors( list, new ArrayList<String>( ) );
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
        // GLaDOS 托管模式：强制禁用自主增殖行为。
        // 放在这里而不是只靠 glados-behaviors.xml，是因为 GladosBehaviors.loadInto()
        // 在存在覆盖文件时是「替换」而非「合并」内置表——用户自定义的
        // --glados-home/glados-behaviors.xml 会让内置的 Frequency=0 声明失效。
        // 在选行为时硬过滤，与配置来源无关，任何覆盖文件都绕不过去。
        if( GladosOptions.isManagedProcess( ) && GladosManagedConstraints.isBreedingBehavior( builder.getName( ) ) )
        {
            return false;
        }

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

    private Map<String, String> getActionSources( )
    {
        return actionSources;
    }

    /** FR-02 诊断用：某个 Action 定义来自哪个文件；未声明时返回 {@code null}。 */
    public String getActionSource( final String name )
    {
        return getActionSources( ).get( name );
    }

    private Map<String, BehaviorBuilder> getBehaviorBuilders( )
    {
        return behaviorBuilders;
    }

    public java.util.Set<String> getBehaviorNames( )
    {
        return behaviorBuilders.keySet( );
    }

    /**
     * FR-08 / D-1 诊断接口：按名字取行为构建器，用于断言后继行为列表是否真正被解析。
     * 仅暴露只读视图，不提供修改入口；不存在时返回 {@code null}。
     */
    public BehaviorBuilder getBehaviorBuilder( final String name )
    {
        return getBehaviorBuilders( ).get( name );
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
