/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.model;


/**
 * An Obstacle actor is used to restrict the walk zone in the scene
 * 
 * @author rgarcia
 */
public class ObstacleActor extends BaseActor {

	public void setVisible(boolean visible) {
		this.visible = visible;
		
		if(scene!= null && scene.getPolygonalNavGraph() != null) {
			if(visible)
				scene.getPolygonalNavGraph().addDinamicObstacle(bbox);
			else
				scene.getPolygonalNavGraph().removeDinamicObstacle(bbox);
		}
	}

	@Override
	public void update(float delta) {
	}

	public void setPosition(float x, float y) {
		boolean inNavGraph = false;
		
		if(scene != null && scene.getPolygonalNavGraph() != null) {
			inNavGraph = scene.getPolygonalNavGraph().removeDinamicObstacle(bbox);
		}
		
		bbox.setPosition(x, y);
		
		if(inNavGraph) {
			scene.getPolygonalNavGraph().addDinamicObstacle(bbox);
		}
	}
}
