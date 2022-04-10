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

import com.io7m.jproperties.JProperties;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.Vector;
import io.vavr.control.Validation;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.sequence;
import static io.vavr.control.Validation.valid;

/**
 * The default generator executor.
 */

@Component(service = ZBlogPostGeneratorExecutorType.class)
public final class ZBlogPostGeneratorExecutor
  implements ZBlogPostGeneratorExecutorType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogPostGeneratorExecutor.class);
  }

  private volatile ZBlogPostSerializerType serializer;
  private volatile ZBlogPostGeneratorResolverType resolver;

  /**
   * Construct an executor.
   */

  public ZBlogPostGeneratorExecutor()
  {
    this.resolver = new ZBlogPostGeneratorResolverSL();
    this.serializer = new ZBlogPostSerializer();
  }

  private static Validation<Seq<ZError>, Properties> loadProperties(
    final Path path)
  {
    LOG.debug("loading properties: {}", path);

    try {
      return valid(JProperties.fromFile(path.toFile()));
    } catch (final IOException e) {
      return invalid(List.of(ZErrors.ofExceptionPath(e, path)));
    }
  }

  private static Validation<Seq<ZError>, SortedMap<Path, ZBlogPost>> runGenerator(
    final ZBlogConfiguration config,
    final ZBlogPostGeneratorType generator,
    final Properties props)
  {
    LOG.debug("executing generator: {}", generator.name());
    return generator.generate(config, props).mapError(List::ofAll);
  }

  private static Validation<Seq<ZError>, Void> serializeFile(
    final ZBlogPost post,
    final String text)
  {
    final Path path = post.path();
    LOG.debug("writing {}", path);

    try {
      Files.write(path, text.getBytes(StandardCharsets.UTF_8));
      return valid(null);
    } catch (final IOException e) {
      return invalid(List.of(ZErrors.ofExceptionPath(e, path)));
    }
  }

  /**
   * Set the generator resolver.
   *
   * @param in_resolver The post generator resolver
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void resolverRegister(
    final ZBlogPostGeneratorResolverType in_resolver)
  {
    this.resolver = Objects.requireNonNull(in_resolver, "resolver");
  }

  /**
   * Set the serializer.
   *
   * @param in_serializer The post serializer
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void serializerRegister(
    final ZBlogPostSerializerType in_serializer)
  {
    this.serializer = Objects.requireNonNull(in_serializer, "serializer");
  }

  private Validation<Seq<ZError>, ZBlogPostGeneratorType> lookupGenerator(
    final String name)
  {
    LOG.debug("looking up generator: {}", name);

    final Optional<ZBlogPostGeneratorType> generator_opt =
      this.resolver.resolve(name);
    return generator_opt.<Validation<Seq<ZError>, ZBlogPostGeneratorType>>
      map(Validation::valid)
      .orElseGet(() -> invalid(List.of(ZErrors.ofMessage("No such generator: " + name))));
  }

  @Override
  public Validation<Seq<ZError>, Void> executeAll(
    final ZBlogConfiguration config)
  {
    Objects.requireNonNull(config, "config");

    final Seq<ZBlogPostGeneratorRequest> values =
      config.generatorRequests().values();

    return sequence(
      values.map(request -> this.lookupGenerator(request.generatorName())
        .flatMap(generator -> loadProperties(request.configFile())
          .flatMap(props -> runGenerator(config, generator, props)
            .flatMap(this::serialize)))))
      .map(x -> (Void) null)
      .mapError(Vector::ofAll);
  }

  private Validation<Seq<ZError>, Void> serialize(
    final SortedMap<Path, ZBlogPost> posts)
  {
    return sequence(
      posts.values()
        .map(post -> this.serializer.serialize(post)
          .flatMap(text -> serializeFile(post, text))
          .mapError(List::ofAll)))
      .flatMap(x -> valid(null));
  }
}
