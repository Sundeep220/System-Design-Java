package com.flowforge.flowforge.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LogExecutionAspect {

    private static final Logger log = LoggerFactory.getLogger(LogExecutionAspect.class);

    // Pointcut 1: Methods annotated with @LogExecution
    @Pointcut("@annotation(com.flowforge.flowforge.aop.LogExecution)")
    public void annotatedMethod() {}

    // Pointcut 2: All methods in classes annotated with @LogExecution
    @Pointcut("@within(com.flowforge.flowforge.aop.LogExecution)")
    public void annotatedClass() {}

    // Pointcut 3: All public methods in the service package
    @Pointcut("execution(public * com.flowforge.flowforge.service..*(..))")
    public void serviceLayer() {}

    @Around("annotatedMethod() || annotatedClass() || serviceLayer()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("→ {}.{}() called with {} arg(s): {}",
                className, methodName, args.length, summarizeArgs(args));

        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("← {}.{}() returned in {} ms", className, methodName, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("✖ {}.{}() threw {} after {} ms: {}",
                    className, methodName, ex.getClass().getSimpleName(), durationMs, ex.getMessage());
            throw ex;
        }
    }

    private String summarizeArgs(Object[] args) {
        if (args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
                .toList()
                .toString();
    }
}
