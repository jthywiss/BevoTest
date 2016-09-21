//
// PolicyWrapper.java -- Java class PolicyWrapper
// Project BevoTest
// https://www.cs.utexas.edu/~jthywiss/bevotest.shtml
//
// Created by jthywiss on Oct 28, 2012.
//
// Copyright (c) 2016 John A. Thywissen. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//     https://www.apache.org/licenses/LICENSE-2.0
//
// This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied. See the License
// for the specific language governing permissions and limitations under
// the License.
//

package edu.utexas.cs.bevotest;

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;
import java.util.Enumeration;

/**
 * A security <code>Policy</code> that wraps another policy and adds given
 * <code>Permissions</code> to it.  Note that this grants permissions
 * globally, to all classes in this JVM instance.  Your security policy may
 * need to be more granular, granting permissions only to certain
 * <code>ProtectionDomain</code>s, in which case this class is not the right
 * solution.
 * <p>
 * Use of this class is equivalent to a security policy configuration file
 * with a default <code>grant</code> entry specifying the given permissions.
 * <p>
 * Example of use:
 * <pre>
 * Policy.setPolicy(new PolicyWrapper(Policy.getPolicy(),
 *     BevoTest.REQUESTED_PERMISSIONS,
 *     PlaintextTestReporter.REQUESTED_PERMISSIONS,
 *     new FilePermission("somefile.txt", "read"),
 *     new FilePermission("anotherfile.txt", "read")
 * ));
 * System.setSecurityManager(new SecurityManager());
 * </pre>
 *
 * @author   John Thywissen
 * @version  $Id$
 * @see      java.security.Policy
 * @see      "Default Policy Implementation and Policy File Syntax"
 */
public class PolicyWrapper extends Policy {
    private final Policy wrappedPolicy;
    final Permissions    addedPermissions;

    /**
     * Constructs a PolicyWrapper for the given Policy, which globally grants
     * the given <code>Permission</code> and
     * <code>PermissionCollection</code>s.
     *
     * @param wrappedPolicy                 the Policy to be wrapped
     * @param addPermissionsAndCollections  varargs or array, each element of
     *                                      which is either a
     *                                      <code>Permission</code> or a
     *                                      <code>PermissionCollection</code>
     * @throws IllegalArgumentException  if any element of the argument is not
     *                                   either a <code>Permission</code> or a
     *                                   <code>PermissionCollection</code>
     * @see   java.security.Permission
     * @see   java.security.PermissionCollection
     */
    public PolicyWrapper(final Policy wrappedPolicy, final Object... addPermissionsAndCollections) {
        super();
        this.wrappedPolicy = wrappedPolicy;
        addedPermissions = new Permissions();
        for (final Object permOrColl : addPermissionsAndCollections) {
            if (permOrColl instanceof PermissionCollection) {
                for (final Enumeration<Permission> pe = ((PermissionCollection) permOrColl).elements(); pe.hasMoreElements();) {
                    addedPermissions.add(pe.nextElement());
                }
            } else if (permOrColl instanceof Permission) {
                addedPermissions.add((Permission) permOrColl);
            } else {
                throw new IllegalArgumentException("PolicyWrapper 2nd and subsequent arguments must each be a Permission or a PermissionCollection");
            }
        }
        addedPermissions.setReadOnly();
    }

    /* (non-Javadoc)
     * @see java.security.Policy#getProvider()
     */
    @Override
    public Provider getProvider() {
        return wrappedPolicy.getProvider();
    }

    /* (non-Javadoc)
     * @see java.security.Policy#getType()
     */
    @Override
    public String getType() {
        return wrappedPolicy.getType();
    }

    /* (non-Javadoc)
     * @see java.security.Policy#getParameters()
     */
    @Override
    public Parameters getParameters() {
        return wrappedPolicy.getParameters();
    }

    /* (non-Javadoc)
     * @see java.security.Policy#getPermissions(java.security.CodeSource)
     */
    @Override
    public PermissionCollection getPermissions(final CodeSource codesource) {
        return wrappedPolicy.getPermissions(codesource);
    }

    /* (non-Javadoc)
     * @see java.security.Policy#getPermissions(java.security.ProtectionDomain)
     */
    @Override
    public PermissionCollection getPermissions(final ProtectionDomain domain) {
        return wrappedPolicy.getPermissions(domain);
    }

    /* (non-Javadoc)
     * @see java.security.Policy#implies(java.security.ProtectionDomain, java.security.Permission)
     */
    @Override
    public boolean implies(final ProtectionDomain domain, final Permission permission) {
        return addedPermissions.implies(permission) || wrappedPolicy.implies(domain, permission);
    }

    /* (non-Javadoc)
     * @see java.security.Policy#refresh()
     */
    @Override
    public void refresh() {
        wrappedPolicy.refresh();
    }

}
