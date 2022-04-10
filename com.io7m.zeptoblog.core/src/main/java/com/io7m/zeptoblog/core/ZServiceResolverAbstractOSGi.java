/*
 * Copyright © 2017 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.zeptoblog.core;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An OSGi resolver implementation.
 *
 * @param <T> The precise type of service
 */

@Component(service = ZServiceResolverType.class)
public abstract class ZServiceResolverAbstractOSGi<T extends ZServiceType>
  implements ZServiceResolverType<T>
{
  private final Map<String, T> services;

  /**
   * Construct a service resolver.
   */

  public ZServiceResolverAbstractOSGi()
  {
    this.services = new ConcurrentHashMap<>();
  }

  /**
   * Introduce a service.
   *
   * @param service The service resolver
   */

  @Reference(
    policyOption = ReferencePolicyOption.GREEDY,
    policy = ReferencePolicy.DYNAMIC,
    cardinality = ReferenceCardinality.MULTIPLE,
    unbind = "serviceUnregister")
  public final void serviceRegister(
    final T service)
  {
    this.services.put(service.name(), service);
  }

  /**
   * Remove a service.
   *
   * @param service The service
   */

  public final void serviceUnregister(
    final T service)
  {
    if (service != null) {
      this.services.remove(service.name(), service);
    }
  }

  @Override
  public final Optional<T> resolve(
    final String name)
  {
    Objects.requireNonNull(name, "name");
    return Optional.ofNullable(this.services.get(name));
  }

  @Override
  public final Set<T> available()
  {
    return HashSet.ofAll(this.services.values());
  }
}
