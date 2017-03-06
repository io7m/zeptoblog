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

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import javaslang.collection.List;
import javaslang.collection.Seq;
import javaslang.collection.SortedMap;
import javaslang.collection.Vector;
import javaslang.control.Validation;
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
import java.util.Optional;
import java.util.Properties;

import static com.io7m.jfunctional.Unit.unit;
import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.sequence;
import static javaslang.control.Validation.valid;

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

  private static Validation<List<ZError>, Properties> loadProperties(
    final Path path)
  {
    LOG.debug("loading properties: {}", path);

    try {
      return valid(JProperties.fromFile(path.toFile()));
    } catch (final IOException e) {
      return invalid(List.of(ZErrors.ofExceptionPath(e, path)));
    }
  }

  private static Validation<List<ZError>, SortedMap<Path, ZBlogPost>> runGenerator(
    final ZBlogConfiguration config,
    final ZBlogPostGeneratorType generator,
    final Properties props)
  {
    LOG.debug("executing generator: {}", generator.name());
    return generator.generate(config, props).mapError(List::ofAll);
  }

  private static Validation<Seq<ZError>, Unit> serializeFile(
    final ZBlogPost post,
    final String text)
  {
    LOG.debug("writing {}", post.path());

    try {
      Files.write(post.path(), text.getBytes(StandardCharsets.UTF_8));
      return valid(unit());
    } catch (final IOException e) {
      return invalid(List.of(ZErrors.ofExceptionPath(e, post.path())));
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
    this.resolver = NullCheck.notNull(in_resolver, "resolver");
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
    this.serializer = NullCheck.notNull(in_serializer, "serializer");
  }

  private Validation<List<ZError>, ZBlogPostGeneratorType> lookupGenerator(
    final String name)
  {
    LOG.debug("looking up generator: {}", name);

    final Optional<ZBlogPostGeneratorType> generator_opt =
      this.resolver.resolve(name);
    if (generator_opt.isPresent()) {
      return valid(generator_opt.get());
    }
    return invalid(List.of(ZErrors.ofMessage("No such generator: " + name)));
  }

  @Override
  public Validation<Seq<ZError>, Unit> executeAll(
    final ZBlogConfiguration config)
  {
    NullCheck.notNull(config, "config");

    return sequence(
      config.generatorRequests().values().map(
        request -> this.lookupGenerator(request.generatorName())
          .flatMap(generator -> loadProperties(request.configFile())
            .flatMap(props -> runGenerator(config, generator, props)
              .flatMap(this::serialize)))))
      .map(x -> unit())
      .mapError(Vector::ofAll);
  }

  private Validation<List<ZError>, Unit> serialize(
    final SortedMap<Path, ZBlogPost> posts)
  {
    return sequence(
      posts.values()
        .map(post -> this.serializer.serialize(post)
          .flatMap(text -> serializeFile(post, text))
          .mapError(List::ofAll)))
      .flatMap(x -> valid(unit()));
  }
}
