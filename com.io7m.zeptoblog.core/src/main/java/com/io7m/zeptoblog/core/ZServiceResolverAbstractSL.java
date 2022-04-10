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

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link ServiceLoader} resolver implementation.
 *
 * @param <T> The precise type of service
 */

@Component(service = ZServiceResolverType.class)
public abstract class ZServiceResolverAbstractSL<T extends ZServiceType>
  implements ZServiceResolverType<T>
{
  private final Map<String, T> services;

  /**
   * Construct a service resolver.
   *
   * @param type The type of service
   */

  public ZServiceResolverAbstractSL(final Class<T> type)
  {
    this.services = new ConcurrentHashMap<>();

    final ServiceLoader<T> loader = ServiceLoader.load(type);
    final Iterator<T> iter = loader.iterator();
    while (iter.hasNext()) {
      final T service = iter.next();
      this.services.put(service.name(), service);
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
