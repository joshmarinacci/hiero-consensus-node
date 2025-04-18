// SPDX-License-Identifier: Apache-2.0
package org.hiero.base.constructable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.hiero.base.io.SelfSerializable;

/**
 * Classes/subclasses annotated with {@link ConstructableIgnored} will not be
 * added to the {@link ConstructableRegistry}.
 * This particularly useful for subclasses or helper classes that implement
 * {@link RuntimeConstructable} but needn't implement
 * {@link SelfSerializable}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstructableIgnored {}
