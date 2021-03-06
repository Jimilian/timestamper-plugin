/*
 * The MIT License
 * 
 * Copyright (c) 2016 Steven G. Brown
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.timestamper.action;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import hudson.plugins.timestamper.format.ElapsedTimestampFormat;
import hudson.plugins.timestamper.format.SystemTimestampFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Unit test for the {@link TimestampsActionQuery} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampsActionQueryTest {

  private static final Optional<Integer> NO_ENDLINE = Optional.absent();

  private static final Optional<String> NO_TIMEZONE = Optional.absent();

  private static final TimestampsActionQuery DEFAULT = new TimestampsActionQuery(
      0, NO_ENDLINE,
      Collections.singletonList(new PrecisionTimestampFormat(3)), false);

  /**
   * @return the test data
   */
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    List<Object[]> testCases = new ArrayList<Object[]>();

    // No query
    testCases.add(new Object[] { "", DEFAULT });
    testCases.add(new Object[] { null, DEFAULT });

    // Precision format
    for (int precision = 0; precision <= 9; precision++) {
      testCases
          .add(new Object[] {
              "precision=" + precision,
              new TimestampsActionQuery(0, NO_ENDLINE, Collections
                  .singletonList(new PrecisionTimestampFormat(precision)),
                  false) });
    }
    List<String> precisionStrings = Arrays.asList("seconds", "milliseconds",
        "microseconds", "nanoseconds");
    for (int i = 0; i < precisionStrings.size(); i++) {
      testCases.add(new Object[] {
          "precision=" + precisionStrings.get(i),
          new TimestampsActionQuery(0, NO_ENDLINE, Collections
              .singletonList(new PrecisionTimestampFormat(i * 3)), false) });
    }
    testCases.addAll(Arrays.asList(new Object[][] {
        { "precision=-1", IllegalArgumentException.class },
        { "precision=invalid", NumberFormatException.class } }));

    // Time format
    testCases.addAll(Arrays.asList(new Object[][] {
        {
            "time=dd:HH:mm:ss",
            new TimestampsActionQuery(0, NO_ENDLINE, Collections
                .singletonList(new SystemTimestampFormat("dd:HH:mm:ss",
                    NO_TIMEZONE, Locale.getDefault())), false) },
        {
            "time=dd:HH:mm:ss&timeZone=GMT+10",
            new TimestampsActionQuery(0, NO_ENDLINE, Collections
                .singletonList(new SystemTimestampFormat("dd:HH:mm:ss",
                    Optional.of("GMT+10"), Locale.getDefault())), false) },
        {
            "time=dd:HH:mm:ss&timeZone=GMT-10",
            new TimestampsActionQuery(0, NO_ENDLINE, Collections
                .singletonList(new SystemTimestampFormat("dd:HH:mm:ss",
                    Optional.of("GMT-10"), Locale.getDefault())), false) },
        {
            "time=EEEE, d MMMM&locale=en",
            new TimestampsActionQuery(0, NO_ENDLINE, Collections
                .singletonList(new SystemTimestampFormat("EEEE, d MMMM",
                    NO_TIMEZONE, Locale.ENGLISH)), false) },
        {
            "time=EEEE, d MMMM&locale=de",
            new TimestampsActionQuery(0, NO_ENDLINE, Collections
                .singletonList(new SystemTimestampFormat("EEEE, d MMMM",
                    NO_TIMEZONE, Locale.GERMAN)), false) } }));

    // Elapsed format
    testCases.add(new Object[] {
        "elapsed=s.SSS",
        new TimestampsActionQuery(0, NO_ENDLINE, Collections
            .singletonList(new ElapsedTimestampFormat("s.SSS")), false) });

    // Multiple formats
    testCases
        .addAll(Arrays.asList(new Object[][] {
            {
                "precision=0&precision=1",
                new TimestampsActionQuery(0, NO_ENDLINE, ImmutableList.of(
                    new PrecisionTimestampFormat(0),
                    new PrecisionTimestampFormat(1)), false) },
            {
                "time=dd:HH:mm:ss&elapsed=s.SSS",
                new TimestampsActionQuery(0, NO_ENDLINE, ImmutableList.of(
                    new SystemTimestampFormat("dd:HH:mm:ss", NO_TIMEZONE,
                        Locale.getDefault()), new ElapsedTimestampFormat(
                        "s.SSS")), false) } }));

    // Start line and end line
    List<Optional<Integer>> lineValues = ImmutableList.of(Optional.of(-1),
        Optional.of(0), Optional.of(1), Optional.<Integer> absent());
    for (Optional<Integer> startLine : lineValues) {
      for (Optional<Integer> endLine : lineValues) {
        List<String> params = new ArrayList<String>();
        if (startLine.isPresent()) {
          params.add("startLine=" + startLine.get());
        }
        if (endLine.isPresent()) {
          params.add("endLine=" + endLine.get());
        }
        String query = Joiner.on('&').join(params);

        if (!query.isEmpty()) {
          testCases.add(new Object[] {
              query,
              new TimestampsActionQuery(startLine.or(0), endLine,
                  DEFAULT.timestampFormats, false) });
        }
      }
    }
    testCases.addAll(Arrays.asList(new Object[][] {
        { "startLine=invalid", NumberFormatException.class },
        { "endLine=invalid", NumberFormatException.class } }));

    // Append log line
    Map<String, Boolean> appendLogParams = ImmutableMap.of("appendLog", true,
        "appendLog=true", true, "appendLog=false", false);
    for (Map.Entry<String, Boolean> mapEntry : appendLogParams.entrySet()) {
      String appendLogParam = mapEntry.getKey();
      boolean appendLog = mapEntry.getValue();

      testCases
          .addAll(Arrays.asList(new Object[][] {
              {
                  appendLogParam,
                  new TimestampsActionQuery(0, NO_ENDLINE,
                      DEFAULT.timestampFormats, appendLog) },
              {
                  "precision=0&" + appendLogParam,
                  new TimestampsActionQuery(0, NO_ENDLINE, Collections
                      .singletonList(new PrecisionTimestampFormat(0)),
                      appendLog) },
              {
                  appendLogParam + "&precision=0",
                  new TimestampsActionQuery(0, NO_ENDLINE, Collections
                      .singletonList(new PrecisionTimestampFormat(0)),
                      appendLog) } }));
    }

    return testCases;
  }

  /**
   */
  @Parameter(0)
  public String queryString;

  /**
   */
  @Parameter(1)
  public Object expectedResult;

  /**
   */
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   */
  @Test
  public void testCreate() {
    if (expectedResult instanceof Class<?>) {
      @SuppressWarnings("unchecked")
      Class<? extends Throwable> expectedThrowable = (Class<? extends Throwable>) expectedResult;
      thrown.expect(expectedThrowable);
    }
    TimestampsActionQuery query = TimestampsActionQuery.create(queryString);
    assertThat(query, is(expectedResult));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreate_changeCaseOfQueryParameterNames() throws Exception {
    queryString = changeCaseOfQueryParameterNames(queryString);
    testCreate();
  }

  /**
   */
  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(TimestampsActionQuery.class)
        .suppress(Warning.NULL_FIELDS).verify();
  }

  /**
   * Change the case of all query parameter names.
   * 
   * @param query
   * @return the modified query
   */
  private String changeCaseOfQueryParameterNames(String query) {
    if (Strings.isNullOrEmpty(query)) {
      return query;
    }

    Pattern paramNamePattern = Pattern.compile("(^|\\&)(.+?)(\\=|\\&|$)");
    Matcher m = paramNamePattern.matcher(query);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String name = m.group();
      name = (name.toLowerCase().equals(name) ? name.toUpperCase() : name
          .toLowerCase());
      m.appendReplacement(sb, name);
    }
    m.appendTail(sb);
    String result = sb.toString();

    if (result.equals(query)) {
      throw new IllegalStateException(
          "Invalid test. No changes made to query: " + query);
    }
    return result;
  }
}
