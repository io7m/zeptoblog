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

package com.io7m.zeptoblog.cmdline;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import java.util.Objects;
import com.io7m.jproperties.JProperties;
import com.io7m.zeptoblog.core.ZBlog;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogConfigurations;
import com.io7m.zeptoblog.core.ZBlogParserProvider;
import com.io7m.zeptoblog.core.ZBlogParserProviderType;
import com.io7m.zeptoblog.core.ZBlogParserType;
import com.io7m.zeptoblog.core.ZBlogPostFormatResolverSL;
import com.io7m.zeptoblog.core.ZBlogPostGeneratorExecutor;
import com.io7m.zeptoblog.core.ZBlogPostGeneratorExecutorType;
import com.io7m.zeptoblog.core.ZBlogPostGeneratorResolverSL;
import com.io7m.zeptoblog.core.ZBlogRendererProvider;
import com.io7m.zeptoblog.core.ZBlogRendererProviderType;
import com.io7m.zeptoblog.core.ZBlogRendererType;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

/**
 * Command-line frontend.
 */

public final class ZBlogMain implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogMain.class);
  }

  private final Map<String, CommandType> commands;
  private final JCommander commander;
  private final String[] args;
  private int exit_code;

  private ZBlogMain(
    final String[] in_args)
  {
    this.args = Objects.requireNonNull(in_args);

    final CommandRoot r = new CommandRoot();
    final CommandCompile compile = new CommandCompile();
    final CommandFormats formats = new CommandFormats();
    final CommandGenerators generators = new CommandGenerators();

    this.commands = new HashMap<>(8);
    this.commands.put("compile", compile);
    this.commands.put("formats", formats);
    this.commands.put("generators", generators);

    this.commander = new JCommander(r);
    this.commander.setProgramName("zeptoblog");
    this.commander.addCommand("compile", compile);
    this.commander.addCommand("formats", formats);
    this.commander.addCommand("generators", generators);
  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   */

  public static void main(final String[] args)
  {
    final ZBlogMain cm = new ZBlogMain(args);
    cm.run();
    System.exit(cm.exitCode());
  }

  private static void show(
    final ZError error)
  {
    LOG.error(error.show());
    error.error().ifPresent(ex -> LOG.error("exception: ", ex));
  }

  /**
   * @return The program exit code
   */

  public int exitCode()
  {
    return this.exit_code;
  }

  @Override
  public void run()
  {
    try {
      this.commander.parse(this.args);

      final String cmd = this.commander.getParsedCommand();
      if (cmd == null) {
        final StringBuilder sb = new StringBuilder(128);
        this.commander.usage();
        LOG.info("Arguments required.\n{}", sb.toString());
        return;
      }

      final CommandType command = this.commands.get(cmd);
      command.call();

    } catch (final ParameterException e) {
      LOG.error("error: ", e);
      this.commander.usage();
      this.exit_code = 1;
    } catch (final Exception e) {
      LOG.error("{}", e.getMessage(), e);
      this.exit_code = 1;
    }
  }

  private interface CommandType extends Callable<Void>
  {

  }

  private class CommandRoot implements CommandType
  {
    @Parameter(
      names = "-verbose",
      converter = ZLogLevelConverter.class,
      description = "Set the minimum logging verbosity level")
    private ZLogLevel verbose = ZLogLevel.LOG_INFO;

    CommandRoot()
    {

    }

    @Override
    public Void call()
      throws Exception
    {
      final ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
          Logger.ROOT_LOGGER_NAME);
      root.setLevel(this.verbose.toLevel());
      return null;
    }
  }

  @Parameters(commandDescription = "List supported post formats")
  private final class CommandFormats extends CommandRoot
  {
    CommandFormats()
    {

    }

    @Override
    public Void call()
      throws Exception
    {
      super.call();

      new ZBlogPostFormatResolverSL().available().forEach(
        p -> System.out.printf("%-32s : %s\n", p.name(), p.description()));
      return null;
    }
  }

  @Parameters(commandDescription = "List supported page generators")
  private final class CommandGenerators extends CommandRoot
  {
    CommandGenerators()
    {

    }

    @Override
    public Void call()
      throws Exception
    {
      super.call();

      new ZBlogPostGeneratorResolverSL().available().forEach(
        p -> System.out.printf("%-32s : %s\n", p.name(), p.description()));
      return null;
    }
  }

  @Parameters(commandDescription = "Compile a blog")
  private final class CommandCompile extends CommandRoot
  {
    @Parameter(
      names = "-config",
      required = true,
      description = "The configuration file")
    private String config_file_in;

    CommandCompile()
    {

    }


    @Override
    public Void call()
      throws Exception
    {
      super.call();

      final File config_file =
        new File(this.config_file_in);
      final Properties config_props =
        JProperties.fromFile(config_file);
      final Validation<Seq<ZError>, ZBlogConfiguration> cr =
        ZBlogConfigurations.fromProperties(config_file.toPath(), config_props);

      if (!cr.isValid()) {
        ZBlogMain.this.exit_code = 1;
        cr.getError().forEach(ZBlogMain::show);
      }

      final ZBlogConfiguration config = cr.get();
      final ZBlogPostGeneratorExecutorType exec = new ZBlogPostGeneratorExecutor();
      final Validation<Seq<ZError>, Void> er = exec.executeAll(config);
      if (!er.isValid()) {
        ZBlogMain.this.exit_code = 1;
        er.getError().forEach(ZBlogMain::show);
        return null;
      }

      final ZBlogParserProviderType blog_provider = new ZBlogParserProvider();
      final ZBlogParserType blog_parser = blog_provider.createParser(config);
      final Validation<Seq<ZError>, ZBlog> br = blog_parser.parse();
      if (!br.isValid()) {
        ZBlogMain.this.exit_code = 1;
        br.getError().forEach(ZBlogMain::show);
        return null;
      }

      final ZBlogRendererProviderType blog_writer_provider =
        new ZBlogRendererProvider();
      final ZBlogRendererType blog_writer =
        blog_writer_provider.createRenderer(config);

      final ZBlog blog = br.get();
      final Validation<Seq<ZError>, Void> wr = blog_writer.render(blog);
      if (!wr.isValid()) {
        ZBlogMain.this.exit_code = 1;
        wr.getError().forEach(ZBlogMain::show);
        return null;
      }

      LOG.debug("done");
      return null;
    }
  }
}
