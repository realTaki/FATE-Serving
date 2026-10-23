package com.webank.ai.fate.serving.core;

import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.validation.DataBinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** Local regressions for CVE-2022-22968; no application or external service is started. */
public class SpringDataBindingSecurityTest {
    @Test
    public void disallowedFieldIsCaseInsensitive() {
        Account account = new Account();
        DataBinder binder = new DataBinder(account);
        binder.setDisallowedFields("secret");
        binder.bind(new MutablePropertyValues().add("Secret", "changed").add("label", "allowed"));
        assertEquals("original", account.getSecret());
        assertEquals("allowed", account.getLabel());
    }

    @Test
    public void nestedDisallowedFieldIsCaseInsensitive() {
        Request request = new Request();
        DataBinder binder = new DataBinder(request);
        binder.setDisallowedFields("account.secret");
        binder.bind(new MutablePropertyValues().add("account.Secret", "changed")
                .add("account.label", "allowed"));
        assertEquals("original", request.getAccount().getSecret());
        assertEquals("allowed", request.getAccount().getLabel());
    }

    @Test
    public void applicationContextCanRegisterAndResolveBean() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            Account account = new Account();
            context.registerBean("account", Account.class, () -> account);
            context.refresh();
            assertSame(account, context.getBean(Account.class));
        }
    }

    public static class Account {
        private String secret = "original";
        private String label;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class Request {
        private Account account = new Account();
        public Account getAccount() { return account; }
        public void setAccount(Account account) { this.account = account; }
    }
}
