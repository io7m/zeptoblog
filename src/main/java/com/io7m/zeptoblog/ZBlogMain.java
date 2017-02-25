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

package com.io7m.zeptoblog;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import javaslang.collection.Seq;
import javaslang.control.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import static com.io7m.jfunctional.Unit.unit;

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
    this.args = NullCheck.notNull(in_args);

    final CommandRoot r = new CommandRoot();
    final CommandCompile compile = new CommandCompile();

    this.commands = new HashMap<>(8);
    this.commands.put("compile", compile);

    this.commander = new JCommander(r);
    this.commander.setProgramName("smf");
    this.commander.addCommand("compile", compile);
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
    LOG.error(error.message());
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
        this.commander.usage(sb);
        LOG.info("Arguments required.\n{}", sb.toString());
        return;
      }

      final CommandType command = this.commands.get(cmd);
      command.call();

    } catch (final ParameterException e) {
      final StringBuilder sb = new StringBuilder(128);
      this.commander.usage(sb);
      LOG.error("{}\n{}", e.getMessage(), sb.toString());
      this.exit_code = 1;
    } catch (final Exception e) {
      LOG.error("{}", e.getMessage(), e);
      this.exit_code = 1;
    }
  }

  private interface CommandType extends Callable<Unit>
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
    public Unit call()
      throws Exception
    {
      final ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
          Logger.ROOT_LOGGER_NAME);
      root.setLevel(this.verbose.toLevel());
      return unit();
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
    public Unit call()
      throws Exception
    {
      super.call();

      final File config_file =
        new File(this.config_file_in);
      final Properties config_props =
        JProperties.fromFile(config_file);
      final Validation<Seq<ZError>, ZBlogConfiguration> cr =
        ZBlogConfigurations.fromProperties(config_file.toPath(), config_props);

      if (cr.isValid()) {
        final ZBlogConfiguration config = cr.get();
        final ZBlogParserProvider blog_provider = new ZBlogParserProvider();
        blog_provider.setBlogPostParserProvider(new ZBlogPostParserProvider());
        final ZBlogParserType blog_parser = blog_provider.createParser(config);
        final Validation<Seq<ZError>, ZBlog> br = blog_parser.parse();
        if (br.isValid()) {
          final ZBlog blog = br.get();
          final ZBlogWriterProvider blog_writer_provider =
            new ZBlogWriterProvider();
          final ZBlogWriterType blog_writer =
            blog_writer_provider.createWriter(config);

          final Validation<Seq<ZError>, Unit> wr = blog_writer.write(blog);
          if (wr.isValid()) {
            LOG.debug("done");
          } else {
            ZBlogMain.this.exit_code = 1;
            wr.getError().forEach(ZBlogMain::show);
          }
        } else {
          ZBlogMain.this.exit_code = 1;
          br.getError().forEach(ZBlogMain::show);
        }
      } else {
        ZBlogMain.this.exit_code = 1;
        cr.getError().forEach(ZBlogMain::show);
      }

      return unit();
    }
  }
}
