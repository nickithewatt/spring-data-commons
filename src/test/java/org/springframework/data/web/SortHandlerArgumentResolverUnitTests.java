/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link SortHandlerMethodArgumentResolver}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public class SortHandlerArgumentResolverUnitTests extends SortDefaultUnitTests {

	static final String SORT_0 = "username";
	static final String SORT_1 = "username,asc";
	static final String[] SORT_2 = new String[] { "username,ASC", "lastname,firstname,DESC" };
	static final String SORT_3 = "firstname,lastname";

	@Test
	public void discoversSimpleSortFromRequest() {

		MethodParameter parameter = getParameterOfMethod("simpleDefault");
		Sort reference = new Sort("bar", "foo");
		NativeWebRequest request = getRequestWithSort(reference);

		assertSupportedAndResolvedTo(request, parameter, reference);
	}

	@Test
	public void discoversComplexSortFromRequest() {

		MethodParameter parameter = getParameterOfMethod("simpleDefault");
		Sort reference = new Sort("bar", "foo").and(new Sort("fizz", "buzz"));

		assertSupportedAndResolvedTo(getRequestWithSort(reference), parameter, reference);
	}

	@Test
	public void discoversQualifiedSortFromRequest() {

		MethodParameter parameter = getParameterOfMethod("qualifiedSort");
		Sort reference = new Sort("bar", "foo");

		assertSupportedAndResolvedTo(getRequestWithSort(reference, "qual"), parameter, reference);
	}

	@Test
	public void returnsNullForSortParameterSetToNothing() throws Exception {

		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", (String) null);

		SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
		Sort result = resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
		assertThat(result, is(nullValue()));
	}

	@Test
	public void buildsUpRequestParameters() {
		assertUriStringFor(SORT, "sort=firstname,lastname,desc");
		assertUriStringFor(new Sort(ASC, "foo").and(new Sort(DESC, "bar").and(new Sort(ASC, "foobar"))),
				"sort=foo,asc&sort=bar,desc&sort=foobar,asc");
		assertUriStringFor(new Sort(ASC, "foo").and(new Sort(ASC, "bar").and(new Sort(DESC, "foobar"))),
				"sort=foo,bar,asc&sort=foobar,desc");
	}

	private void assertUriStringFor(Sort sort, String expected) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		new SortHandlerMethodArgumentResolver().enhance(builder, parameter, sort);

		assertThat(builder.build().toUriString(), endsWith(expected));
	}

	private static void assertSupportedAndResolvedTo(NativeWebRequest request, MethodParameter parameter, Sort sort) {

		SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter), is(true));

		try {
			assertThat(resolver.resolveArgument(parameter, null, request, null), is(sort));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static NativeWebRequest getRequestWithSort(Sort sort) {

		return getRequestWithSort(sort, null);
	}

	private static NativeWebRequest getRequestWithSort(Sort sort, String qualifier) {

		MockHttpServletRequest request = new MockHttpServletRequest();

		if (sort == null) {
			return new ServletWebRequest(request);
		}

		for (Order order : sort) {

			String prefix = StringUtils.hasText(qualifier) ? qualifier + "_" : "";
			request.addParameter(prefix + "sort", String.format("%s,%s", order.getProperty(), order.getDirection().name()));
		}

		return new ServletWebRequest(request);
	}

	@Override
	protected Class<?> getControllerClass() {
		return Controller.class;
	}

	interface Controller {

		void supportedMethod(Sort sort);

		void unsupportedMethod(String string);

		void qualifiedSort(@Qualifier("qual") Sort sort);

		void simpleDefault(@SortDefault({ "firstname", "lastname" }) Sort sort);

		void simpleDefaultWithDirection(
				@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC) Sort sort);

		void containeredDefault(@SortDefaults(@SortDefault({ "foo", "bar" })) Sort sort);

		void invalid(@SortDefaults(@SortDefault({ "foo", "bar" })) @SortDefault({ "bar", "foo" }) Sort sort);
	}
}
