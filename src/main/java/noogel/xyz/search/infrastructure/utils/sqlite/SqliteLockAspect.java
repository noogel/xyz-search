package noogel.xyz.search.infrastructure.utils.sqlite;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class SqliteLockAspect {

    @Pointcut("@annotation(noogel.xyz.search.infrastructure.utils.sqlite.SqliteLock)")
    public void pointCut() {

    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        synchronized (SqliteLockAspect.class) {
            return pjp.proceed();
        }
    }
}
