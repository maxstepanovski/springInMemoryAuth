package com.example.demo

import com.example.demo.data.AuthRepository
import com.example.demo.filters.JwtAuthenticationFilter
import com.example.demo.filters.JwtAuthorizationFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.PasswordEncoder
import javax.crypto.SecretKey
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
class AuthSecurityConfiguration : WebSecurityConfigurerAdapter() {

    @Value("jwt.issuer")
    lateinit var jwtIssuer: String

    @Value("jwt.audience")
    lateinit var jwtAudience: String

    @Value("jwt.type")
    lateinit var jwtType: String

    @Autowired
    lateinit var secretKey: SecretKey

    @Autowired
    lateinit var authRepository: AuthRepository

    @Autowired
    lateinit var dataSource: DataSource

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder


    override fun configure(auth: AuthenticationManagerBuilder) {
        auth
                .jdbcAuthentication()
                .dataSource(dataSource)
                .passwordEncoder(passwordEncoder)
                .rolePrefix("ROLE_")
                .usersByUsernameQuery(
                        "SELECT user_name, user_password, enabled " +
                                "FROM authentication.user WHERE user_name = ?"
                )
                .authoritiesByUsernameQuery(
                        "select authentication.user.user_name, authentication.authority.authority " +
                                "from authentication.authority, authentication.user " +
                                "where authentication.authority.user_id = authentication.user.id and user_name = ?"
                )
    }

    override fun configure(http: HttpSecurity) {
        http
//                .cors()
//                .and()
                .httpBasic()
                .and()
//                .csrf()
//                .disable()
//                .addFilter(
//                        JwtAuthorizationFilter(
//                                authRepository, secretKey, authenticationManager()
//                        )
//                )
//                .addFilter(
//                        JwtAuthenticationFilter(
//                                jwtAudience, jwtIssuer, jwtType, secretKey, authenticationManager()
//                        )
//                )
//                .sessionManagement()
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
                .authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/register").permitAll()
                .antMatchers("/${Role.ADMIN.alias}/*").hasRole(Role.ADMIN.alias)
                .antMatchers("/${Role.USER.alias}/*").hasRole(Role.USER.alias)
                .antMatchers("/common/*").hasAnyRole(Role.ADMIN.alias, Role.USER.alias)
    }
}

enum class Role(val alias: String) {
    ADMIN("admin"), USER("user")
}