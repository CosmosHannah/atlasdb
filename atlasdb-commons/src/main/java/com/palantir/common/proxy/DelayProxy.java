/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.common.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.palantir.common.base.Throwables;

public class DelayProxy implements DelegatingInvocationHandler {

    @SuppressWarnings("unchecked")
    public static <T> T newProxyInstance(Class<T> interfaceClass, T delegate,
            Supplier<Long> sleepTimeMsSupplier) {
        return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(),
            new Class<?>[] {interfaceClass}, new DelayProxy(delegate, sleepTimeMsSupplier));
    }

    public static <T> T newProxyInstance(Class<T> interfaceClass, T delegate, long sleepTimeMs) {
        return newProxyInstance(interfaceClass, delegate, Suppliers.ofInstance(sleepTimeMs));
    }

    private final Object delegate;
    private final Supplier<Long> sleepTimeMsSupplier;

    public DelayProxy(Object delegate, Supplier<Long> sleepSupplier) {
        this.delegate = delegate;
        this.sleepTimeMsSupplier = sleepSupplier;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long sleepTimeMs = sleepTimeMsSupplier.get();
        try {
            Thread.sleep((sleepTimeMs + 1) / 2);
        } catch (InterruptedException e) {
            throw Throwables.throwUncheckedException(e);
        }

        Object retVal;
        try {
            retVal = method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            try {
                Thread.sleep(sleepTimeMs / 2);
            } catch (InterruptedException e2) {
                Thread.currentThread().interrupt();
            }
            throw e.getCause();
        }

        try {
            Thread.sleep(sleepTimeMs / 2);
        } catch (InterruptedException e) {
            throw Throwables.throwUncheckedException(e);
        }
        return retVal;
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }
}
