/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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


import com.io7m.jnull.NullCheck;
import javaslang.collection.HashSet;
import javaslang.collection.Set;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An OSGi resolver implementation.
 */

@Component(service = ZBlogPostFormatResolverType.class)
public final class ZBlogPostFormatResolverOSGi implements
  ZBlogPostFormatResolverType
{
  private final ConcurrentHashMap<String, ZBlogPostFormatProviderType> formats;

  /**
   * Construct a format provider resolver.
   */

  public ZBlogPostFormatResolverOSGi()
  {
    this.formats = new ConcurrentHashMap<>();
  }

  /**
   * Introduce a blog post parser provider.
   *
   * @param provider The post parser provider
   */

  @Reference(
    policyOption = ReferencePolicyOption.GREEDY,
    policy = ReferencePolicy.DYNAMIC,
    cardinality = ReferenceCardinality.MULTIPLE,
    unbind = "formatProviderUnregister")
  public void formatProviderRegister(
    final ZBlogPostFormatProviderType provider)
  {
    this.formats.put(provider.name(), provider);
  }

  /**
   * Remove a blog post parser post provider.
   *
   * @param provider The post parser provider
   */

  public void formatProviderUnregister(
    final ZBlogPostFormatProviderType provider)
  {
    if (provider != null) {
      this.formats.remove(provider.name(), provider);
    }
  }

  @Override
  public Optional<ZBlogPostFormatProviderType> resolve(
    final String name)
  {
    NullCheck.notNull(name, "name");
    return Optional.ofNullable(this.formats.get(name));
  }

  @Override
  public Set<ZBlogPostFormatProviderType> formats()
  {
    return HashSet.ofAll(this.formats.values());
  }
}
