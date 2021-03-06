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
package com.bladecoder.engineeditor.ui.components;

import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engineeditor.Ctx;

public class ActorInputPanel extends EditableOptionsInputPanel<String> {
	ActorInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory));
		
		if(mandatory)
			setText(Ctx.project.getSelectedActor().getId());
	}

	private static String[] getValues(boolean mandatory) {
		HashMap<String, BaseActor> actors = Ctx.project.getSelectedScene().getActors();
		
		String[] result = new String[actors.size()];
		
		BaseActor[] v = actors.values().toArray(new BaseActor[actors.size()]);
		
		for(int i = 0; i < actors.size(); i++) {
			result[i] = v[i].getId();
		}
		
		Arrays.sort(result);
		return result;
	}
}
