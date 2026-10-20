package com.sunset.eveil.global.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 메서드의 AuthenticatedClient 파라미터에 인터셉터가 넣어 둔 값을 주입한다.
 */
@Component
public class ClientAuthArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return AuthenticatedClient.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Object client = webRequest.getAttribute(AuthenticatedClient.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		if (client == null) {
			throw new ClientAuthenticationException();
		}
		return client;
	}
}
