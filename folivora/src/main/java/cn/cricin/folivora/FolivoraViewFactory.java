/*
 * Copyright (C) 2019 Cricin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.cricin.folivora;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.reflect.Field;

/**
 * A view factory takes responsibility of view creation, if the view
 * is created, folivora will create a drawable for it if available.
 */
final class FolivoraViewFactory implements LayoutInflater.Factory2 {
  private static final String[] sClassPrefixList = {
    "android.widget.",
    "android.webkit.",
    "android.app.",
    "android.view."
  };

  LayoutInflater.Factory2 mFactory2;
  LayoutInflater.Factory mFactory;

  @Override
  public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
    View result = null;
    if (mFactory2 != null) {
      result = mFactory2.onCreateView(parent, name, context, attrs);
    }
    if (mFactory != null && result == null) {
      result = mFactory.onCreateView(name, context, attrs);
    }
    if(name.endsWith("ViewStub")) return null;//fix an NPE when creating ViewStub

    if (result == null) {
      LayoutInflater inflater = getLayoutInflater(context);
      for (String prefix : sClassPrefixList) {
        try {
          result = inflater.createView(name, prefix, attrs);
          if (result != null) break;
        } catch (ClassNotFoundException e) {
          // In this case we want to let the LayoutInflater self take a crack
          // at it.
        }
      }
    }

    if (result != null) {
      Folivora.applyDrawableToView(result, attrs);
    }
    return result;
  }

  private LayoutInflater getLayoutInflater(Context context) {
    LayoutInflater inflater = LayoutInflater.from(context);
    if (inflater instanceof FolivoraInflater) {
      return ((FolivoraInflater) inflater).getBaseInflater();
    } else {
      return inflater;
    }
  }

  @Override
  public View onCreateView(String name, Context context, AttributeSet attrs) {
    return onCreateView(null, name, context, attrs);
  }

  private static boolean sCheckedField;
  private static Field sLayoutInflaterFactory2Field;

  static void forceSetFactory2(LayoutInflater inflater, LayoutInflater.Factory2 factory) {
    if (!sCheckedField) {
      try {
        //noinspection JavaReflectionMemberAccess
        sLayoutInflaterFactory2Field = LayoutInflater.class.getDeclaredField("mFactory2");
        sLayoutInflaterFactory2Field.setAccessible(true);
      } catch (NoSuchFieldException e) {
        Log.e("Folivora", "forceSetFactory2 Could not find field 'mFactory2' on class "
          + inflater.getClass().getName() + "; Folivora will not available.", e);
      }
      sCheckedField = true;
    }
    if (sLayoutInflaterFactory2Field != null) {
      try {
        sLayoutInflaterFactory2Field.set(inflater, factory);
      } catch (Exception e) {
        Log.e("Folivora", "forceSetFactory2 could not set the Factory2 on LayoutInflater "
          + inflater + "; Folivora will not available.", e);
      }
    }
  }

}
