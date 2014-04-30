package org.bladecoder.engine.model;

import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.ActionCallbackSerialization;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.AnimationStateListener;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;

public class SpriteSpineRenderer implements SpriteRenderer {

	private HashMap<String, FrameAnimation> fanims = new HashMap<String, FrameAnimation>();

	/** Starts this anim the first time that the scene is loaded */
	private String initFrameAnimation;
	private FrameAnimation currentFrameAnimation;
	
	private ActionCallback animationCb = null;
	private String animationCbSer = null;
	
	private int currentCount;
	private int currentAnimationType;

	private boolean flipX;

	private SkeletonCacheEntry currentSkeleton;

	private SkeletonRenderer renderer;
	private SkeletonBounds bounds;

	private HashMap<String, SkeletonCacheEntry> skeletonCache = new HashMap<String, SkeletonCacheEntry>();

	class SkeletonCacheEntry {
		int refCounter;
		Skeleton skeleton;
		AnimationState animation;
	}
	
	private AnimationStateListener animationListener = new AnimationStateListener() {
		@Override
		public void complete(int trackIndex, int loopCount) {
			if(currentCount >= loopCount) {
				// TODO Manage loopCount				
			}
			
			if (animationCb != null || animationCbSer != null) {

				if (animationCb == null) {
					animationCb = ActionCallbackSerialization
							.find(animationCbSer);
					animationCbSer = null;
				}

				ActionCallbackQueue.add(animationCb);
				animationCb = null;
			}			
		}

		@Override
		public void end(int arg0) {
		}

		@Override
		public void event(int arg0, Event arg1) {
		}

		@Override
		public void start(int arg0) {
		}		
	};

	@Override
	public void addFrameAnimation(FrameAnimation fa) {
		if (initFrameAnimation == null)
			initFrameAnimation = fa.id;

		fanims.put(fa.id, fa);
	}

	@Override
	public String getCurrentFrameAnimationId() {
		if (currentFrameAnimation == null)
			return null;

		String id = currentFrameAnimation.id;

		if (flipX) {
			id = FrameAnimation.getFlipId(id);
		}

		return id;

	}

	@Override
	public void setInitFrameAnimation(String fa) {
		initFrameAnimation = fa;
	}

	@Override
	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}
	
	@Override
	public String[] getInternalAnimations(String source) {
		retrieveSource(source);
		
		Array<Animation> animations = skeletonCache.get(source).skeleton.getData().getAnimations();
		String[] result = new String[animations.size];
		
		for(int i = 0; i< animations.size; i++) {
			Animation a = animations.get(i);
			result[i] = a.getName();
		}
		
		return result;
	}	

	@Override
	public void update(float delta) {
		currentSkeleton.animation.update(delta);

		currentSkeleton.animation.apply(currentSkeleton.skeleton);
		currentSkeleton.skeleton.updateWorldTransform();

		bounds.update(currentSkeleton.skeleton, true);
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale) {

		currentSkeleton.skeleton.setX(x);
		currentSkeleton.skeleton.setY(y);

		renderer.draw(batch, currentSkeleton.skeleton);
	}

	@Override
	public float getWidth() {
		return bounds.getWidth();
	}

	@Override
	public float getHeight() {
		return bounds.getHeight();
	}

	@Override
	public FrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	@Override
	public void lookat(Vector2 p0, Vector2 pf) {
		lookat(FrameAnimation.getFrameDirection(p0, pf));
	}

	@Override
	public void lookat(String direction) {
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.STAND_ANIM);
		sb.append('.');
		sb.append(direction);

		startFrameAnimation(sb.toString(), EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void stand() {
		String standFA = FrameAnimation.STAND_ANIM;
		int idx = getCurrentFrameAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentFrameAnimationId().substring(idx);
		}

		startFrameAnimation(standFA, EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		String currentDirection = FrameAnimation.getFrameDirection(p0, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.WALK_ANIM).append('.').append(currentDirection);
		startFrameAnimation(sb.toString(), EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		FrameAnimation fa = getFrameAnimation(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}
		
		if(currentFrameAnimation != null && currentFrameAnimation.disposeWhenPlayed)
			disposeSource(currentFrameAnimation.source);

		currentFrameAnimation = fa;
		currentSkeleton = skeletonCache.get(fa.source);

		animationCb = cb;

		// If the source is not loaded. Load it.
		if (currentFrameAnimation != null
				&& currentSkeleton.refCounter < 1) {
			loadSource(fa.source);
			EngineAssetManager.getInstance().getManager().finishLoading();

			retrieveSource(fa.source);
		}

		if (repeatType == EngineTween.FROM_FA) {
			currentAnimationType = currentFrameAnimation.animationType;
			currentCount = currentFrameAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		currentSkeleton.skeleton.setFlipX(flipX);
		currentSkeleton.animation.setAnimation(0, id, currentAnimationType == EngineTween.REPEAT);
	}
	
	private FrameAnimation getFrameAnimation(String id) {
		FrameAnimation fa = fanims.get(id);
		flipX = false;

		if (fa == null) {
			// Search for flipped
			String flipId = FrameAnimation.getFlipId(id);

			fa = fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();

				if (id.endsWith(FrameAnimation.LEFT)) {
					sb.append(id.substring(0, id.length() - 4));
					sb.append("frontleft");
				} else if (id.endsWith(FrameAnimation.FRONTLEFT)) {
					sb.append(id.substring(0, id.length() - 9));
					sb.append("left");
				} else if (id.endsWith(FrameAnimation.RIGHT)) {
					sb.append(id.substring(0, id.length() - 5));
					sb.append("frontright");
				} else if (id.endsWith(FrameAnimation.FRONTRIGHT)) {
					sb.append(id.substring(0, id.length() - 10));
					sb.append("right");
				}

				String s = sb.toString();

				fa = fanims.get(s);

				if (fa == null) {
					// Search for flipped
					flipId = FrameAnimation.getFlipId(s);

					fa = fanims.get(flipId);

					if (fa != null)
						flipX = true;
				}
			}
		}

		return fa;
	}	

	private void loadSource(String source) {
		SkeletonCacheEntry entry = skeletonCache.get(source);
		
		if(entry == null) {
			entry = new SkeletonCacheEntry();
			skeletonCache.put(source, entry);
		}

		if (entry.refCounter == 0)
			EngineAssetManager.getInstance().loadAtlas(source);

		entry.refCounter++;
	}

	private void retrieveSource(String source) {
		SkeletonCacheEntry entry = skeletonCache.get(source);
		
		if(entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().getManager().finishLoading();
			entry = skeletonCache.get(source);
		}

		if (entry.skeleton == null) {
			TextureAtlas atlas = EngineAssetManager.getInstance()
					.getTextureAtlas(source);

			SkeletonJson json = new SkeletonJson(atlas);
			SkeletonData skeletonData = json
					.readSkeletonData(EngineAssetManager.getInstance()
							.getSpine(source));
			
			entry.skeleton = new Skeleton(skeletonData);	

			AnimationStateData stateData = new AnimationStateData(skeletonData); // Defines mixing between animations.
			stateData.setDefaultMix(0);

			entry.animation = new AnimationState(stateData);			
			entry.animation.addListener(animationListener);
		}
	}
	
	public void disposeSource(String source) {
		SkeletonCacheEntry entry = skeletonCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeAtlas(source);
			entry.animation = null;
			entry.skeleton = null;
		}

		entry.refCounter--;
	}

	@Override
	public void loadAssets() {
		for (FrameAnimation fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source);
		}

		if (currentFrameAnimation != null && !currentFrameAnimation.preload) {
			loadSource(currentFrameAnimation.source);
		} else if (currentFrameAnimation == null && initFrameAnimation != null) {
			FrameAnimation fa = fanims.get(initFrameAnimation);

			if (!fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {
		for (String key : skeletonCache.keySet()) {
			if(skeletonCache.get(key).refCounter > 0)
				retrieveSource(key);
		}

		if (currentFrameAnimation != null) {
			SkeletonCacheEntry entry = skeletonCache.get(currentFrameAnimation.source);
			currentSkeleton = entry;
			
			// TODO RESTORE CURRENT ANIMATION STATE

		} else if(initFrameAnimation != null){
			startFrameAnimation(initFrameAnimation, EngineTween.FROM_FA, 1,
					null);
		}

		renderer = new SkeletonRenderer();
		renderer.setPremultipliedAlpha(true);

		bounds = new SkeletonBounds();
	}

	@Override
	public void dispose() {
		for (String key : skeletonCache.keySet()) {
			EngineAssetManager.getInstance().disposeAtlas(key);
		}

		skeletonCache.clear();
		currentSkeleton = null;
		renderer = null;
		bounds = null;
	}

	@Override
	public void write(Json json) {
		// TODO Auto-generated method stub

	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		// TODO Auto-generated method stub

	}
}