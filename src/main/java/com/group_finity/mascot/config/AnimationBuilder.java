package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.sound.SoundLoader;
/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class AnimationBuilder {

	private static final Logger log = Logger.getLogger(AnimationBuilder.class.getName());

	private final String condition;
	
	private String imageSet = "";

	private final List<Pose> poses = new ArrayList<Pose>();

	public AnimationBuilder(final Entry animationNode, final String imageSet) throws IOException {
		if( !imageSet.equals("") )
			this.imageSet = imageSet;
		
		this.condition = animationNode.getAttribute("Condition") == null ? "true" : animationNode.getAttribute("Condition");

		log.log(Level.INFO, "Start Reading Animations");

		for (final Entry frameNode : animationNode.getChildren()) {
			this.getPoses().add(loadPose(frameNode));
		}

		log.log(Level.INFO, "Animations Finished Loading");
	}

	private Pose loadPose(final Entry frameNode) throws IOException {

		final String imageText = imageSet+frameNode.getAttribute("Image");
		final String imageRightText = frameNode.getAttribute("ImageRight") != null ? imageSet+frameNode.getAttribute("ImageRight") : null;
		final String anchorText = frameNode.getAttribute("ImageAnchor");
		final String moveText = frameNode.getAttribute("Velocity");
		final String durationText = frameNode.getAttribute("Duration");
		final String soundText = frameNode.getAttribute("Sound") != null ? imageSet + "/sound" + frameNode.getAttribute("Sound") : null;
		final String volumeText = frameNode.getAttribute("Volume") != null ? frameNode.getAttribute("Volume") : "0";

		final String[] anchorCoordinates = anchorText.split(",");
		final Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));

		try {
			ImagePairLoader.load(imageText, imageRightText, anchor);
		} catch( Exception e ) {
			log.log(Level.SEVERE, "Failed to load image: "+imageText);
			throw new IOException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadImageErrorMessage" ) + imageText );
		}

		final String[] moveCoordinates = moveText.split(",");
		final Point move = new Point(Integer.parseInt(moveCoordinates[0]), Integer.parseInt(moveCoordinates[1]));

		final int duration = Integer.parseInt(durationText);
                
                if( soundText != null )
                {
                    try
                    {
                        SoundLoader.load( soundText, Float.parseFloat( volumeText ) );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Failed to load sound: " + soundText );
                        throw new IOException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedLoadSoundErrorMessage" ) + soundText );
                    }
                }

		final Pose pose = new Pose( imageText, imageRightText, move.x, move.y, duration, soundText != null ? soundText + Float.parseFloat( volumeText ) : null );
		
		log.log(Level.INFO, "ReadPosition({0})", pose);
		
		return pose;
	}

	public Animation buildAnimation() throws AnimationInstantiationException {
		try {
			return new Animation(Variable.parse(this.getCondition()), this.getPoses().toArray(new Pose[0]));
		} catch (final VariableException e) {
			throw new AnimationInstantiationException( Main.getInstance( ).getLanguageBundle( ).getProperty( "FailedConditionEvaluationErrorMessage" ), e);
		}
	}
	
	private List<Pose> getPoses() {
		return this.poses;
	}
	
	private String getCondition() {
		return this.condition;
	}
}
