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

package com.jogamp.opengl.util.glsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;

import jogamp.opengl.Debug;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.util.GLArrayDataEditable;

/**
 * ShaderState allows to sharing data between shader programs,
 * while updating the attribute and uniform locations when switching.
 * <p>
 * This allows seamless switching of programs using <i>almost</i> same data
 * but performing different artifacts. 
 * </p>
 * <p>
 * A {@link #useProgram(GL2ES2, boolean) used} ShaderState is attached to the current GL context
 * and can be retrieved via {@link #getShaderState(GL)}.
 * </p>
 */
public class ShaderState {
    public static final boolean DEBUG = Debug.isPropertyDefined("jogl.debug.GLSLState", true);
    
    public ShaderState() {
    }

    public boolean verbose() { return verbose; }

    public void setVerbose(boolean v) { verbose = DEBUG || v; }

    /**
     * Returns the attached user object for the given name to this ShaderState.
     */
    public final Object getAttachedObject(String name) {
      return attachedObjectsByString.get(name);
    }

    /**
     * Attach user object for the given name to this ShaderState.
     * Returns the previously set object or null.
     * 
     * @return the previous mapped object or null if none
     */
    public final Object attachObject(String name, Object obj) {
      return attachedObjectsByString.put(name, obj);
    }

    /**
     * @param name name of the mapped object to detach
     * 
     * @return the previous mapped object or null if none
     */
    public final Object detachObject(String name) {
        return attachedObjectsByString.remove(name);
    }    
    
    /**
     * Turns the shader program on or off.<br>
     *
     * @throws GLException if no program is attached
     *
     * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
     */
    public synchronized void useProgram(GL2ES2 gl, boolean on) throws GLException {
        if(null==shaderProgram) { throw new GLException("No program is attached"); }        
        if(on) {
            if(shaderProgram.linked()) {
                shaderProgram.useProgram(gl, true);
                if(resetAllShaderData) {
                    resetAllAttributes(gl);
                    resetAllUniforms(gl);
                }
            } else { 
                if(resetAllShaderData) {
                    setAllAttributes(gl);
                }
                if(!shaderProgram.link(gl, System.err)) {
                    throw new GLException("could not link program: "+shaderProgram);
                }
                shaderProgram.useProgram(gl, true);
                if(resetAllShaderData) {
                    resetAllUniforms(gl);
                }
            }
            resetAllShaderData = false;            
        } else {
            shaderProgram.useProgram(gl, false);
        }
    }

    public boolean linked() {
        return (null!=shaderProgram)?shaderProgram.linked():false;
    }

    public boolean inUse() {
        return (null!=shaderProgram)?shaderProgram.inUse():false;
    }

    /**
     * Attach or switch a shader program
     *
     * <p>Attaching a shader program the first time, 
     * as well as switching to another program on the fly,
     * while managing all attribute and uniform data.</p>
     * 
     * <p>[Re]sets all data and use program in case of a program switch.</p>
     *  
     * <p>Use program, {@link #useProgram(GL2ES2, boolean)},
     * if <code>enable</code> is <code>true</code>.</p>
     * 
     * @return true if shader program was attached, otherwise false (already attached)
     * 
     * @throws GLException if program was not linked and linking fails
     */
    public synchronized boolean attachShaderProgram(GL2ES2 gl, ShaderProgram prog, boolean enable) throws GLException {
        if(verbose) {
            int curId = (null!=shaderProgram)?shaderProgram.id():-1;
            int newId = (null!=prog)?prog.id():-1;
            System.err.println("ShaderState: attachShaderProgram: "+curId+" -> "+newId+" (enable: "+enable+")\n\t"+shaderProgram+"\n\t"+prog);
            if(DEBUG) {
                Thread.dumpStack();
            }                    
        }
        if(null!=shaderProgram) {
            if(shaderProgram.equals(prog)) {
                if(enable) {
                    useProgram(gl, true);
                }
                // nothing else to do ..
                if(verbose) {
                    System.err.println("ShaderState: attachShaderProgram: No switch, equal id: "+shaderProgram.id()+", enabling "+enable);
                }
                return false;
            }
            if(shaderProgram.inUse()) {
                if(null != prog && enable) {
                    // new program will issue glUseProgram(..)
                    shaderProgram.programInUse = false;
                } else {
                    // no new 'enabled' program - disable
                    useProgram(gl, false);
                }
            }
            resetAllShaderData = true;
        }

        // register new one
        shaderProgram = prog;

        if(null!=shaderProgram) {
            // [re]set all data and use program if switching program, 
            // or  use program if program is linked
            if(resetAllShaderData || enable) {
                useProgram(gl, true); // may reset all data
                if(!enable) {
                    useProgram(gl, false);
                }
            }
        }
        if(DEBUG) {
            System.err.println("Info: attachShaderProgram: END");
        }
        return true;
    }

    public ShaderProgram shaderProgram() { return shaderProgram; }

    /**
     * Calls {@link #release(GL2ES2, boolean, boolean, boolean) release(gl, true, true, true)}
     *
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean, boolean)
     */
    public synchronized void destroy(GL2ES2 gl) {
        release(gl, true, true, true);
        attachedObjectsByString.clear();        
    }

    /**
     * Calls {@link #release(GL2ES2, boolean, boolean, boolean) release(gl, false, false, false)}
     *
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see #release(GL2ES2, boolean, boolean, boolean)
     */
    public synchronized void releaseAllData(GL2ES2 gl) {
        release(gl, false, false, false);
    }

    /**
     * @see #glReleaseAllVertexAttributes
     * @see #glReleaseAllUniforms
     * @see ShaderProgram#release(GL2ES2, boolean)
     */
    public synchronized void release(GL2ES2 gl, boolean destroyBoundAttributes, boolean destroyShaderProgram, boolean destroyShaderCode) {
        if(null!=shaderProgram) {            
            shaderProgram.useProgram(gl, false);
        }
        if(destroyBoundAttributes) {
            for(Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
                iter.next().destroy(gl);
            }            
        }
        releaseAllAttributes(gl);
        releaseAllUniforms(gl);
        if(null!=shaderProgram && destroyShaderProgram) {
            shaderProgram.release(gl, destroyShaderCode);
        }
    }

    //
    // Shader attribute handling
    //

    /**
     * Gets the cached location of a shader attribute.
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     *
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #getAttribLocation(GL2ES2, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int getCachedAttribLocation(String name) {
        Integer idx = activeAttribLocationMap.get(name);
        return (null!=idx)?idx.intValue():-1;
    }
    
    /**
     * Get the previous cached vertex attribute data.
     *
     * @return the GLArrayData object, null if not previously set.
     *
     * @see #ownAttribute(GLArrayData, boolean)
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
     */
    public GLArrayData getAttribute(String name) {
        return activeAttribDataMap.get(name);
    }
    
    public boolean isActiveAttribute(GLArrayData attribute) {
        return attribute == activeAttribDataMap.get(attribute.getName());
    }
    
    /**
     * Binds or unbinds the {@link GLArrayData} lifecycle to this ShaderState.
     *  
     * <p>If an attribute location is cached (ie {@link #bindAttribLocation(GL2ES2, int, String)})
     * it is promoted to the {@link GLArrayData} instance.</p>
     * 
     * <p>The attribute will be destroyed with {@link #destroy(GL2ES2)} 
     * and it's location will be reset when switching shader with {@link #attachShaderProgram(GL2ES2, ShaderProgram)}.</p>
     *  
     * <p>The data will not be transfered to the GPU, use {@link #vertexAttribPointer(GL2ES2, GLArrayData)} additionally.</p>
     * 
     * <p>The data will also be {@link GLArrayData#associate(Object, boolean) associated} with this ShaderState.</p>
     * 
     * @param attribute the {@link GLArrayData} which lifecycle shall be managed
     * @param own true if <i>owning</i> shall be performs, false if <i>disowning</i>.
     * 
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see #getAttribute(String)
     * @see GLArrayData#associate(Object, boolean)
     */
    public void ownAttribute(GLArrayData attribute, boolean own) {
        if(own) {
            final int location = getCachedAttribLocation(attribute.getName());
            if(0<=location) {
                attribute.setLocation(location);
            }
            managedAttributes.add(managedAttributes.size(), attribute);
        } else {
            managedAttributes.remove(attribute);
        }
        attribute.associate(this, own);
    }
    
    public boolean ownsAttribute(GLArrayData attribute) {
        return managedAttributes.contains(attribute);
    }
    
    /**
     * Binds a shader attribute to a location.
     * Multiple names can be bound to one location.
     * The value will be cached and can be retrieved via {@link #getCachedAttribLocation(String)}
     * before or after linking.
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is already linked
     * 
     * @see javax.media.opengl.GL2ES2#glBindAttribLocation(int, int, String)
     * @see #getAttribLocation(GL2ES2, String)
     * @see #getCachedAttribLocation(String)
     */
    public void bindAttribLocation(GL2ES2 gl, int location, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(shaderProgram.linked()) throw new GLException("Program is already linked");        
        final Integer loc = new Integer(location);
        activeAttribLocationMap.put(name, loc);
        gl.glBindAttribLocation(shaderProgram.program(), location, name);
    }

    /**
     * Binds a shader {@link GLArrayData} attribute to a location.
     * Multiple names can be bound to one location.
     * The value will be cached and can be retrieved via {@link #getCachedAttribLocation(String)}
     * and {@link #getAttribute(String)}before or after linking.
     * The {@link GLArrayData}'s location will be set as well.
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is already linked
     * 
     * @see javax.media.opengl.GL2ES2#glBindAttribLocation(int, int, String)
     * @see #getAttribLocation(GL2ES2, String)
     * @see #getCachedAttribLocation(String)
     * @see #getAttribute(String)
     */
    public void bindAttribLocation(GL2ES2 gl, int location, GLArrayData data) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        if(shaderProgram.linked()) throw new GLException("Program is already linked");
        final String name = data.getName();
        final Integer loc = new Integer(location);
        activeAttribLocationMap.put(name, loc);
        data.setLocation(gl, shaderProgram.program(), location);
        activeAttribDataMap.put(data.getName(), data);
    }

    /**
     * Gets the location of a shader attribute.<br>
     * Uses either the cached value {@link #getCachedAttribLocation(String)} if valid,
     * or the GLSL queried via {@link GL2ES2#glGetAttribLocation(int, String)}.<br>
     * The location will be cached.
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #getCachedAttribLocation(String)
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     */
    public int getAttribLocation(GL2ES2 gl, String name) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        int location = getCachedAttribLocation(name);
        if(0>location) {
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = gl.glGetAttribLocation(shaderProgram.program(), name);
            if(0<=location) {
                Integer idx = new Integer(location);
                activeAttribLocationMap.put(name, idx);
                if(DEBUG) {
                    System.err.println("ShaderState: glGetAttribLocation: "+name+", loc: "+location);
                }
            } else if(verbose) {
                System.err.println("ShaderState: glGetAttribLocation failed, no location for: "+name+", loc: "+location);
                if(DEBUG) {
                    Thread.dumpStack();
                }                    
            }
        }
        return location;
    }

    /**
     * Validates and returns the location of a shader attribute.<br>
     * Uses either the cached value {@link #getCachedAttribLocation(String)} if valid, 
     * or the GLSL queried via {@link GL2ES2#glGetAttribLocation(int, String)}.<br>
     * The location will be cached and set in the  
     * {@link GLArrayData} object.
     *
     * @return -1 if there is no such attribute available, 
     *         otherwise >= 0
     *         
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #getCachedAttribLocation(String)
     * @see #bindAttribLocation(GL2ES2, int, GLArrayData)
     * @see #bindAttribLocation(GL2ES2, int, String)
     * @see GL2ES2#glGetAttribLocation(int, String)
     * @see #getAttribute(String)
     */
    public int getAttribLocation(GL2ES2 gl, GLArrayData data) {
        if(null==shaderProgram) throw new GLException("No program is attached");
        final String name = data.getName();
        int location = getCachedAttribLocation(name);
        if(0<=location) {
            data.setLocation(location);
        } else {
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = data.setLocation(gl, shaderProgram.program());
            if(0<=location) {
                Integer idx = new Integer(location);
                activeAttribLocationMap.put(name, idx);
                if(DEBUG) {
                    System.err.println("ShaderState: glGetAttribLocation: "+name+", loc: "+location);
                }
            } else if(verbose) {
                System.err.println("ShaderState: glGetAttribLocation failed, no location for: "+name+", loc: "+location);
                if(DEBUG) {
                    Thread.dumpStack();
                }                    
            }
        }        
        activeAttribDataMap.put(data.getName(), data);
        return location;
    }
    
    //
    // Enabled Vertex Arrays and its data
    //

    /**
     * @return true if the named attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(String name) {
        final Boolean v = activedAttribEnabledMap.get(name);
        return null != v && v.booleanValue();
    }
    
    /**
     * @return true if the {@link GLArrayData} attribute is enable
     */
    public final boolean isVertexAttribArrayEnabled(GLArrayData data) {
        return isVertexAttribArrayEnabled(data.getName());
    }
    
    private boolean enableVertexAttribArray(GL2ES2 gl, String name, int location) {
        activedAttribEnabledMap.put(name, Boolean.TRUE);
        if(0>location) {
            location = getAttribLocation(gl, name);
            if(0>location) {
                if(verbose) {
                    System.err.println("ShaderState: glEnableVertexAttribArray failed, no index for: "+name);
                    if(DEBUG) {
                        Thread.dumpStack();
                    }                    
                }
                return false;
            }
        }
        if(DEBUG) {
            System.err.println("ShaderState: glEnableVertexAttribArray: "+name+", loc: "+location);
        }
        gl.glEnableVertexAttribArray(location);
        return true;
    }
    
    /**
     * Enables a vertex attribute array.
     * 
     * This method retrieves the the location via {@link #getAttribLocation(GL2ES2, GLArrayData)}
     * hence {@link #enableVertexAttribArray(GL2ES2, GLArrayData)} shall be preferred. 
     *
     * Even if the attribute is not found in the current shader,
     * it is marked enabled in this state.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not linked and no location was cached.
     * 
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean enableVertexAttribArray(GL2ES2 gl, String name) {
        return enableVertexAttribArray(gl, name, -1);
    }
    

    /**
     * Enables a vertex attribute array, usually invoked by {@link GLArrayDataEditable#enableBuffer(GL, boolean)}.
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #enableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #getAttribLocation(GL2ES2, GLArrayData)} set
     * and cached in this state.
     *  
     * Even if the attribute is not found in the current shader,
     * it is marked enabled in this state.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see GLArrayDataEditable#enableBuffer(GL, boolean)
     */
    public boolean enableVertexAttribArray(GL2ES2 gl, GLArrayData data) {
        if(0 > data.getLocation()) {
            getAttribLocation(gl, data);
        } else {
            // ensure data is the current bound one
            activeAttribDataMap.put(data.getName(), data);             
        }
        return enableVertexAttribArray(gl, data.getName(), data.getLocation());
    }
    
    private boolean disableVertexAttribArray(GL2ES2 gl, String name, int location) {
        activedAttribEnabledMap.put(name, Boolean.FALSE);
        if(0>location) {
            location = getAttribLocation(gl, name);
            if(0>location) {
                if(verbose) {
                    System.err.println("ShaderState: glDisableVertexAttribArray failed, no index for: "+name);
                    if(DEBUG) {
                        Thread.dumpStack();
                    }
                }
                return false;
            }
        }
        if(DEBUG) {
            System.err.println("ShaderState: glDisableVertexAttribArray: "+name);
        }
        gl.glDisableVertexAttribArray(location);
        return true;
    }
    
    /**
     * Disables a vertex attribute array
     *
     * This method retrieves the the location via {@link #getAttribLocation(GL2ES2, GLArrayData)}
     * hence {@link #disableVertexAttribArray(GL2ES2, GLArrayData)} shall be preferred.
     *  
     * Even if the attribute is not found in the current shader,
     * it is removed from this state enabled list.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean disableVertexAttribArray(GL2ES2 gl, String name) {
        return disableVertexAttribArray(gl, name, -1);
    }

    /**
     * Disables a vertex attribute array
     *
     * This method uses the {@link GLArrayData}'s location if set
     * and is the preferred alternative to {@link #disableVertexAttribArray(GL2ES2, String)}.
     * If data location is unset it will be retrieved via {@link #getAttribLocation(GL2ES2, GLArrayData)} set
     * and cached in this state.
     *  
     * Even if the attribute is not found in the current shader,
     * it is removed from this state enabled list.
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean disableVertexAttribArray(GL2ES2 gl, GLArrayData data) {
        if(0 > data.getLocation()) {
            getAttribLocation(gl, data);
        }
        return disableVertexAttribArray(gl, data.getName(), data.getLocation());
    }
    
    /**
     * Set the {@link GLArrayData} vertex attribute data.
     * 
     * This method uses the {@link GLArrayData}'s location if set.
     * If data location is unset it will be retrieved via {@link #getAttribLocation(GL2ES2, GLArrayData)}, set
     * and cached in this state.
     * 
     * @return false, if the location could not be determined, otherwise true
     *
     * @throws GLException if no program is attached
     * @throws GLException if the program is not linked and no location was cached.
     * 
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     */
    public boolean vertexAttribPointer(GL2ES2 gl, GLArrayData data) {
        int location = data.getLocation();
        if(0 > location) {
            location = getAttribLocation(gl, data);
        } 
        if(0 <= location) {
            // only pass the data, if the attribute exists in the current shader
            if(DEBUG) {
                System.err.println("ShaderState: glVertexAttribPointer: "+data);
            }
            gl.glVertexAttribPointer(data);
            return true;
        }
        return false;
    }

    /**
     * Releases all mapped vertex attribute data,
     * disables all enabled attributes and loses all indices
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
     */
    public void releaseAllAttributes(GL2ES2 gl) {
        if(null!=shaderProgram) {
            for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
                disableVertexAttribArray(gl, iter.next());
            }
            for(Iterator<String> iter = activedAttribEnabledMap.keySet().iterator(); iter.hasNext(); ) {
                disableVertexAttribArray(gl, iter.next());
            }
        }
        activeAttribDataMap.clear();
        activedAttribEnabledMap.clear();
        activeAttribLocationMap.clear();
        managedAttributes.clear();        
    }
        
    /**
     * Disables all vertex attribute arrays.
     *
     * Their enabled stated will be removed from this state only
     * if 'removeFromState' is true.
     *
     * This method purpose is more for debugging. 
     *
     * @see #glEnableVertexAttribArray
     * @see #glDisableVertexAttribArray
     * @see #glVertexAttribPointer
     * @see #getVertexAttribPointer
     * @see #glReleaseAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see #glResetAllVertexAttributes
     * @see ShaderProgram#glReplaceShader
     */
    public void disableAllVertexAttributeArrays(GL2ES2 gl, boolean removeFromState) {
        for(Iterator<String> iter = activedAttribEnabledMap.keySet().iterator(); iter.hasNext(); ) {
            final String name = iter.next();
            if(removeFromState) {
                activedAttribEnabledMap.remove(name);
            }
            final int index = getAttribLocation(gl, name);
            if(0<=index) {
                gl.glDisableVertexAttribArray(index);
            }
        }
    }

    private final void relocateAttribute(GL2ES2 gl, GLArrayData attribute) {
        // get new location ..
        final String name = attribute.getName();
        final int loc = getAttribLocation(gl, name);
        attribute.setLocation(loc);

        if(0<=loc) {
            if(isVertexAttribArrayEnabled(name)) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(loc);
            }
    
            if( attribute.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, attribute.getVBOName());
                gl.glVertexAttribPointer(attribute);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            } else {   
                gl.glVertexAttribPointer(attribute);
            }
        }
    }
    
    /**
     * Reset all previously enabled mapped vertex attribute data.
     * 
     * <p>Attribute data is bound to the GL state</p>
     * <p>Attribute location is bound to the program</p>
     * 
     * <p>However, since binding an attribute to a location via {@link #bindAttribLocation(GL2ES2, int, GLArrayData)}
     * <i>must</i> happen before linking <b>and</b> we try to promote the attributes to the new program,
     * we have to gather the probably new location etc.</p>
     *
     * @throws GLException is the program is not linked
     *
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    private final void resetAllAttributes(GL2ES2 gl) {
        if(!shaderProgram.linked()) throw new GLException("Program is not linked");
        activeAttribLocationMap.clear();
        
        for(int i=0; i<managedAttributes.size(); i++) {
            ((GLArrayData)managedAttributes.get(i)).setLocation(-1);
        }
        for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            relocateAttribute(gl, iter.next());
        }
    }

    private final void setAttribute(GL2ES2 gl, GLArrayData attribute) {
        // get new location ..
        final String name = attribute.getName();
        final int loc = attribute.getLocation();

        if(0<=loc) {
            bindAttribLocation(gl, loc, name);
            
            if(isVertexAttribArrayEnabled(name)) {
                // enable attrib, VBO and pass location/data
                gl.glEnableVertexAttribArray(loc);
            }
    
            if( attribute.isVBO() ) {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, attribute.getVBOName());
                gl.glVertexAttribPointer(attribute);
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            } else {   
                gl.glVertexAttribPointer(attribute);
            }
        }
    }
    
    /**
     * preserves the attribute location .. (program not linked)
     */
    private final void setAllAttributes(GL2ES2 gl) {
        for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            setAttribute(gl, iter.next());
        }
    }

    //
    // Shader Uniform handling
    //

    /**
     * Gets the cached location of the shader uniform.
     *
     * @return -1 if there is no such uniform available, 
     *         otherwise >= 0
     */
    public final int getCachedUniformLocation(String name) {
        Integer idx = (Integer) activeUniformLocationMap.get(name);
        return (null!=idx)?idx.intValue():-1;
    }

    /**
     * Bind the {@link GLUniform} lifecycle to this ShaderState.
     *  
     * <p>If a uniform location is cached it is promoted to the {@link GLUniformData} instance.</p>
     * 
     * <p>The attribute will be destroyed with {@link #destroy(GL2ES2)} 
     * and it's location will be reset when switching shader with {@link #attachShaderProgram(GL2ES2, ShaderProgram)}.</p>
     *  
     * <p>The data will not be transfered to the GPU, use {@link #uniform(GL2ES2, GLUniformData)} additionally.</p>
     * 
     * @param uniform the {@link GLUniformData} which lifecycle shall be managed
     * 
     * @see #getUniform(String)
     */
    public void ownUniform(GLUniformData uniform) {
        final int location = getCachedUniformLocation(uniform.getName());
        if(0<=location) {
            uniform.setLocation(location);
        }        
        activeUniformDataMap.put(uniform.getName(), uniform);
        managedUniforms.add(uniform);        
    }
    
    public boolean ownsUniform(GLUniformData uniform) {
        return managedUniforms.contains(uniform);
    }
    
    /**
     * Gets the location of a shader uniform.<br>
     * Uses either the cached value {@link #getCachedUniformLocation(String)} if valid,
     * or the GLSL queried via {@link GL2ES2#glGetUniformLocation(int, String)}.<br>
     * The location will be cached.
     * <p>
     * The current shader program ({@link #attachShaderProgram(GL2ES2, ShaderProgram)}) 
     * must be in use ({@link #useProgram(GL2ES2, boolean) }) !</p>
     *
     * @return -1 if there is no such attribute available,
     *         otherwise >= 0

     * @throws GLException is the program is not linked
     *
     * @see #glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glGetUniformLocation
     * @see #getUniformLocation
     * @see ShaderProgram#glReplaceShader
     */
    public final int getUniformLocation(GL2ES2 gl, String name) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int location = getCachedUniformLocation(name);
        if(0>location) {
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = gl.glGetUniformLocation(shaderProgram.program(), name);
            if(0<=location) {
                Integer idx = new Integer(location);
                activeUniformLocationMap.put(name, idx);
            } else if(verbose) {
                System.err.println("ShaderState: glUniform failed, no location for: "+name+", index: "+location);
                if(DEBUG) {
                    Thread.dumpStack();
                }
            }
        }
        return location;
    }
   
    /**
     * Validates and returns the location of a shader uniform.<br>
     * Uses either the cached value {@link #getCachedUniformLocation(String)} if valid,
     * or the GLSL queried via {@link GL2ES2#glGetUniformLocation(int, String)}.<br>
     * The location will be cached and set in the  
     * {@link GLUniformData} object.
     * <p>
     * The current shader program ({@link #attachShaderProgram(GL2ES2, ShaderProgram)}) 
     * must be in use ({@link #useProgram(GL2ES2, boolean) }) !</p>
     *
     * @return -1 if there is no such attribute available,
     *         otherwise >= 0

     * @throws GLException is the program is not linked
     *
     * @see #glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glGetUniformLocation
     * @see #getUniformLocation
     * @see ShaderProgram#glReplaceShader
     */
    public int getUniformLocation(GL2ES2 gl, GLUniformData data) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        final String name = data.getName();
        int location = getCachedUniformLocation(name);
        if(0<=location) {
            data.setLocation(location);
        } else {
            if(!shaderProgram.linked()) throw new GLException("Program is not linked");
            location = data.setLocation(gl, shaderProgram.program());
            if(0<=location) {
                Integer idx = new Integer(location);
                activeUniformLocationMap.put(name, idx);
            } else if(verbose) {
                System.err.println("ShaderState: glUniform failed, no location for: "+name+", index: "+location);
                if(DEBUG) {
                    Thread.dumpStack();
                }
            }
        }        
        activeUniformDataMap.put(name, data);        
        return location;
    }
    
    /**
     * Set the uniform data.
     *
     * Even if the uniform is not found in the current shader,
     * it is stored in this state.
     *
     * @param data the GLUniforms's name must match the uniform one,
     *      it's index will be set with the uniforms's location,
     *      if found.
     *
     *
     * @return false, if the name is not found, otherwise true
     *
     * @throws GLException if the program is not in use
     *
     * @see #glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glGetUniformLocation
     * @see javax.media.opengl.GL2ES2#glUniform
     * @see #getUniformLocation
     * @see ShaderProgram#glReplaceShader
     */
    public boolean uniform(GL2ES2 gl, GLUniformData data) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");
        int location = data.getLocation();
        if(0>location) {
            location = getUniformLocation(gl, data);
        }
        if(0<=location) {
            // only pass the data, if the uniform exists in the current shader
            if(DEBUG) {
                System.err.println("ShaderState: glUniform: "+data);
            }
            gl.glUniform(data);
        }
        return true;
    }

    /**
     * Get the uniform data, previously set.
     *
     * @return the GLUniformData object, null if not previously set.
     */
    public GLUniformData getUniform(String name) {
        return activeUniformDataMap.get(name);
    }

    /**
     * Releases all mapped uniform data
     * and loses all indices
     */
    public void releaseAllUniforms(GL2ES2 gl) {
        activeUniformDataMap.clear();
        activeUniformLocationMap.clear();
        managedUniforms.clear();
    }
        
    /**
     * Reset all previously mapped uniform data
     * 
     * Uniform data and location is bound to the program,
     * hence both are updated here
     *
     * @throws GLException is the program is not in use
     * 
     * @see #attachShaderProgram(GL2ES2, ShaderProgram)
     */
    private final void resetAllUniforms(GL2ES2 gl) {
        if(!shaderProgram.inUse()) throw new GLException("Program is not in use");        
        activeUniformLocationMap.clear();
        for(Iterator<GLUniformData> iter = managedUniforms.iterator(); iter.hasNext(); ) {
            iter.next().setLocation(-1);
        }        
        for(Iterator<GLUniformData> iter = activeUniformDataMap.values().iterator(); iter.hasNext(); ) {
            final GLUniformData uniform = iter.next();
            uniform.setLocation(-1);
            uniform(gl, uniform);
        }
    }

    public StringBuilder toString(StringBuilder sb, boolean alsoUnlocated) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        
        sb.append("ShaderState[ ");
        
        sb.append(Platform.getNewline()).append(" ");
        if(null != shaderProgram) {
            shaderProgram.toString(sb);
        } else {
            sb.append("ShaderProgram: null");
        }
        sb.append(Platform.getNewline()).append(" enabledAttributes [");
        {
            Iterator<String> names = activedAttribEnabledMap.keySet().iterator();
            Iterator<Boolean> values = activedAttribEnabledMap.values().iterator();
            while( names.hasNext() ) {
                sb.append(Platform.getNewline()).append("  ").append(names.next()).append(": ").append(values.next());
            }
        }
        sb.append(Platform.getNewline()).append(" ],").append(" activeAttributes [");
        for(Iterator<GLArrayData> iter = activeAttribDataMap.values().iterator(); iter.hasNext(); ) {
            final GLArrayData ad = iter.next();
            if( alsoUnlocated || 0 <= ad.getLocation() ) {
                sb.append(Platform.getNewline()).append("  ").append(ad);
            }
        }
        sb.append(Platform.getNewline()).append(" ],").append(" managedAttributes [");
        for(Iterator<GLArrayData> iter = managedAttributes.iterator(); iter.hasNext(); ) {
            final GLArrayData ad = iter.next();
            if( alsoUnlocated || 0 <= ad.getLocation() ) {
                sb.append(Platform.getNewline()).append("  ").append(ad);
            }
        }
        sb.append(Platform.getNewline()).append(" ],").append(" activeUniforms [");
        for(Iterator<GLUniformData> iter=activeUniformDataMap.values().iterator(); iter.hasNext(); ) {
            final GLUniformData ud = iter.next();
            if( alsoUnlocated || 0 <= ud.getLocation() ) {
                sb.append(Platform.getNewline()).append("  ").append(ud);
            }
        }
        sb.append(Platform.getNewline()).append(" ],").append(" managedUniforms [");
        for(Iterator<GLUniformData> iter = managedUniforms.iterator(); iter.hasNext(); ) {
            final GLUniformData ud = iter.next();
            if( alsoUnlocated || 0 <= ud.getLocation() ) {
                sb.append(Platform.getNewline()).append("  ").append(ud);
            }
        }
        sb.append(Platform.getNewline()).append(" ]").append(Platform.getNewline()).append("]");
        return sb;
    }
    
    @Override
    public String toString() {
        return toString(null, DEBUG).toString();
    }
    
    private boolean verbose = DEBUG;
    private ShaderProgram shaderProgram=null;
    
    private HashMap<String, Boolean> activedAttribEnabledMap = new HashMap<String, Boolean>();
    private HashMap<String, Integer> activeAttribLocationMap = new HashMap<String, Integer>();
    private HashMap<String, GLArrayData> activeAttribDataMap = new HashMap<String, GLArrayData>();
    private ArrayList<GLArrayData> managedAttributes = new ArrayList<GLArrayData>();
    
    private HashMap<String, Integer> activeUniformLocationMap = new HashMap<String, Integer>();
    private HashMap<String, GLUniformData> activeUniformDataMap = new HashMap<String, GLUniformData>();
    private ArrayList<GLUniformData> managedUniforms = new ArrayList<GLUniformData>();
    
    private HashMap<String, Object> attachedObjectsByString = new HashMap<String, Object>();    
    private boolean resetAllShaderData = false;
}

