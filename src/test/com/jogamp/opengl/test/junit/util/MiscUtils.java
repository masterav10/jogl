/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 

package com.jogamp.opengl.test.junit.util;

import java.lang.reflect.*;
import java.nio.FloatBuffer;

public class MiscUtils {
    public static int atoi(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return def;
    }
    
    public static long atol(String str, long def) {
        try {
            return Long.parseLong(str);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return def;
    }

    public static void assertFloatBufferEquals(String errmsg, FloatBuffer expected, FloatBuffer actual, float delta) {
        if(null == expected && null == actual) {
            return;
        }
        String msg = null != errmsg ? errmsg + " " : "";
        if(null == expected) {
            throw new AssertionError(msg+"; Expected is null, but actual not: "+actual);
        }
        if(null == actual) {
            throw new AssertionError(msg+"; Actual is null, but expected not: "+expected);
        }
        if(expected.remaining() != actual.remaining()) {
            throw new AssertionError(msg+"; Expected has "+expected.remaining()+" remaining, but actual has "+actual.remaining());            
        }
        final int a0 = expected.position();
        final int b0 = actual.position();
        for(int i=0; i<expected.remaining(); i++) {
            final float ai = expected.get(a0 + i);
            final float bi = actual.get(b0 + i);
            final float daibi = Math.abs(ai - bi);  
            if( daibi > delta ) {
                throw new AssertionError(msg+"; Expected @ ["+a0+"+"+i+"] has "+ai+", but actual @ ["+b0+"+"+i+"] has "+bi+", it's delta "+daibi+" > "+delta);
            }
        }
    }
    
    public static void assertFloatBufferNotEqual(String errmsg, FloatBuffer expected, FloatBuffer actual, float delta) {
        if(null == expected || null == actual) {
            return;
        }
        if(expected.remaining() != actual.remaining()) {
            return;            
        }
        String msg = null != errmsg ? errmsg + " " : "";
        final int a0 = expected.position();
        final int b0 = actual.position();
        for(int i=0; i<expected.remaining(); i++) {
            final float ai = expected.get(a0 + i);
            final float bi = actual.get(b0 + i);
            final float daibi = Math.abs(ai - bi);  
            if( daibi > delta ) {
                return;
            }
        }
        throw new AssertionError(msg+"; Expected and actual are equal.");
    }
    
    public static boolean setFieldIfExists(Object instance, String fieldName, Object value) {
        try {
            Field f = instance.getClass().getField(fieldName);
            if(value instanceof Boolean || f.getType().isInstance(value)) {
                f.set(instance, value);
                return true;
            } else {
                System.out.println(instance.getClass()+" '"+fieldName+"' field not assignable with "+value.getClass()+", it's a: "+f.getType());
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchFieldException nsfe) {
            // OK - throw new RuntimeException(instance.getClass()+" has no '"+fieldName+"' field", nsfe);
        }
        return false;
    }
}



