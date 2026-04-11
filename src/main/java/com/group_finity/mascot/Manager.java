package com.group_finity.mascot;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import java.awt.Point;

/**
 * 
 * Maintains a list of mascot, the object to time.
 * 
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Manager {

	private static final Logger log = LoggerFactory.getLogger(Manager.class);

	/**
	* Interval timer is running.
	*/
	public static final int TICK_INTERVAL = 40;

	/**
	 * A list of mascot.
	 */
	private final List<Mascot> mascots = new ArrayList<Mascot>();

	/**
	* The mascot will be added later.
	* (@Link ConcurrentModificationException) to prevent the addition of the mascot (@link # tick ()) are each simultaneously reflecting.
	 */
	private final Set<Mascot> added = new LinkedHashSet<Mascot>();

	/**
	* The mascot will be added later.
	* (@Link ConcurrentModificationException) to prevent the deletion of the mascot (@link # tick ()) are each simultaneously reflecting.
	 */
	private final Set<Mascot> removed = new LinkedHashSet<Mascot>();

	private boolean exitOnLastRemoved = true;
	
	private Thread thread;

	public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
		this.exitOnLastRemoved = exitOnLastRemoved;
	}

	public boolean isExitOnLastRemoved() {
		return exitOnLastRemoved;
	}

	public Manager() {

		new Thread() {
			{
				this.setDaemon(true);
				this.start();
			}

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					} catch (final InterruptedException ex) {
					}
				}
			}
		};
	}
	
	public void start() {
		if ( thread!=null && thread.isAlive() ) {
			return;
		}
		
		thread = new Thread() {
			@Override
			public void run() {

				long prev = System.nanoTime() / 1000000;
				try {
					for (;;) {
						for (;;) {
							final long cur = System.nanoTime() / 1000000;
							if (cur - prev >= TICK_INTERVAL) {
								if (cur > prev + TICK_INTERVAL * 2) {
									prev = cur;
								} else {
									prev += TICK_INTERVAL;
								}
								break;
							}
							Thread.sleep(1, 0);
						}

						tick();
					}
				} catch (final InterruptedException e) {
				}
			}
		};
		thread.setDaemon(false);
		
		thread.start();
	}
	
	public void stop() {
		if ( thread==null || !thread.isAlive() ) {
			return;
		}
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
	}

	private void tick( )
        {
            // Update the first environmental information
            NativeFactory.getInstance().getEnvironment().tick();

		synchronized (this.getMascots()) {

			// Add the mascot if it should be added
			for (final Mascot mascot : this.getAdded()) {
				this.getMascots().add(mascot);
			}
			this.getAdded().clear();

			// Remove the mascot if it should be removed
			for (final Mascot mascot : this.getRemoved()) {
				this.getMascots().remove(mascot);
			}
			this.getRemoved().clear();

			// Advance mascot's time
			for (final Mascot mascot : this.getMascots()) {
				mascot.tick();
			}
			
			// Advance mascot's time
			for (final Mascot mascot : this.getMascots()) {
				mascot.apply();
			}
		}

		if (isExitOnLastRemoved()) {
			if (this.getMascots().size() == 0) {
				Main.getInstance().exit();
			}
		}
	}

	public void add(final Mascot mascot) {
		synchronized (this.getAdded()) {
			this.getAdded().add(mascot);
			this.getRemoved().remove(mascot);
		}
		mascot.setManager(this);
	}

	public void remove(final Mascot mascot) {
		synchronized (this.getAdded()) {
			this.getAdded().remove(mascot);
			this.getRemoved().add(mascot);
		}
		mascot.setManager(null);
	}

	public void setBehaviorAll(final String name) {
		synchronized (this.getMascots()) {
			for (final Mascot mascot : this.getMascots()) {
				try {
                                    Configuration configuration = Main.getInstance( ).getConfiguration( mascot.getImageSet( ) );
				    mascot.setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( name ), mascot ) );
				} catch (final BehaviorInstantiationException e) {
					log.error("Failed to initialize the following actions", e);
					Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
					mascot.dispose();
				} catch (final CantBeAliveException e) {
					log.error("Fatal Error", e);
                                        Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
					mascot.dispose();
				}
			}
		}
	}	
	
	public void setBehaviorAll(final Configuration configuration, final String name, String imageSet) {
		synchronized (this.getMascots()) {
			for (final Mascot mascot : this.getMascots()) {
				try {
					if( mascot.getImageSet().equals(imageSet) ) {
						mascot.setBehavior(configuration.buildBehavior( configuration.getSchema( ).getString( name ), mascot ) );						
					}
				} catch (final BehaviorInstantiationException e) {
					log.error("Failed to initialize the following actions", e);
					Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
					mascot.dispose();
				} catch (final CantBeAliveException e) {
					log.error("Fatal Error", e);
					Main.showError( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedSetBehaviourErrorMessage" ) );
					mascot.dispose();
				}
			}
		}
	}

	public void remainOne() {
		synchronized (this.getMascots()) {
			int totalMascots = this.getMascots().size();
			for (int i = totalMascots - 1; i > 0; --i) {
				this.getMascots().get(i).dispose();				
			}
		}
        }
        
	public void remainOne( Mascot mascot )
        {
            synchronized( this.getMascots( ) )
            {
                int totalMascots = this.getMascots( ).size( );
                for( int i = totalMascots - 1; i >= 0; --i )
                {
                    if( !this.getMascots( ).get( i ).equals( mascot ) )
                        this.getMascots( ).get( i ).dispose( );
                }
            }
	}
	
	public void remainOne( String imageSet ) {
		synchronized (this.getMascots()) {
			int totalMascots = this.getMascots().size();
			boolean isFirst = true;
			for (int i = totalMascots - 1; i >= 0; --i) {
				Mascot m = this.getMascots().get(i);
				if( m.getImageSet().equals(imageSet) && isFirst) {
					isFirst = false;
				} else if( m.getImageSet().equals(imageSet) && !isFirst) {
					m.dispose();
				}
			}
		}
	}
	
    public void remainNone( String imageSet )
    {
        synchronized( this.getMascots( ) )
        {
            int totalMascots = this.getMascots( ).size( );
            for( int i = totalMascots - 1; i >= 0; --i )
            {
                Mascot m = this.getMascots( ).get( i );
                if( m.getImageSet( ).equals( imageSet ) )
                    m.dispose( );
            }
        }
    }

    public void togglePauseAll( )
    {
        boolean isPaused = true;
        
        synchronized( this.getMascots( ) )
        {
            for( final Mascot mascot : this.getMascots( ) )
            {
                if( !mascot.isPaused( ) )
                {
                    isPaused = false;
                    break;
                }
            }
            
            for( final Mascot mascot : this.getMascots( ) )
            {
                mascot.setPaused( !isPaused );
            }
        }
    }

    public boolean isPaused( )
    {
        boolean isPaused = true;
        
        synchronized( this.getMascots( ) )
        {
            for( final Mascot mascot : this.getMascots( ) )
            {
                if( !mascot.isPaused( ) )
                {
                    isPaused = false;
                    break;
                }
            }
        }
        
        return isPaused;
    }

    public int getCount( )
    {
        return getCount( null );
    }
    
    public int getCount( String imageSet )
    {
        synchronized( getMascots( ) )
        {
            if( imageSet == null )
            {
                return getMascots( ).size( );
            }
            else   
            {
                int count = 0;
                for( int index = 0; index < getMascots( ).size( ); index++ )
                {
                    Mascot m = getMascots( ).get( index );
                    if( m.getImageSet( ).equals( imageSet ) )
                        count++;
                }
                return count;
            }
        }
    }

	private List<Mascot> getMascots() {
		return this.mascots;
	}

	private Set<Mascot> getAdded() {
		return this.added;
	}

	private Set<Mascot> getRemoved() {
		return this.removed;
	}
        
        /**
         * Returns a Mascot with the given affordance.
         * @param affordance
         * @return A WeakReference to a mascot with the required affordance, or null
         */
        public WeakReference<Mascot> getMascotWithAffordance( String affordance )
        {
            synchronized( this.getMascots( ) )
            {
                for( final Mascot mascot : this.getMascots( ) )
                {
                    if( mascot.getAffordances( ).contains( affordance ) )
                        return new WeakReference<Mascot>( mascot );
                }
            }
            
            return null;
        }

    public boolean hasOverlappingMascotsAtPoint( Point anchor )
    {
        int count = 0;
        
        synchronized( this.getMascots( ) )
        {
            for( final Mascot mascot : this.getMascots( ) )
            {
                if( mascot.getAnchor( ).equals( anchor ) )
                    count++;
                if( count > 1 )
                    return true;
            }
        }

        return false;
    }

	public void disposeAll() {
		synchronized (this.getMascots()) {
			for (int i = this.getMascots().size() - 1; i >= 0; --i) {
				this.getMascots().get(i).dispose();
			}
		}
	}
}
