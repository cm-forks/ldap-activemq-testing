package org.fuse.usecase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.jms.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports = {
        @CreateTransport(protocol = "LDAP", port = 1024) })
@ApplyLdifFiles("org/fuse/usecase/activemq.ldif")
public class LDAPActiveMQTest extends AbstractLdapTestUnit {

    public BrokerService broker;
    public static LdapServer ldapServer;

    @Before public void setup() throws Exception {
        System.setProperty("ldapPort", String.valueOf(getLdapServer().getPort()));
        broker = BrokerFactory.createBroker("xbean:org/fuse/usecase/activemq-broker.xml");
        broker.start();
        broker.waitUntilStarted();
    }

    @After public void shutdown() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }

    @Test public void testFailCreateSessionNotEnoughRights() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost");
        try {
            Connection conn = factory.createQueueConnection("jdoe", "sunflower");
            fail("Expected JMSException");
        } catch (JMSException expected) {
            //fail("User jdoe is not authorized to create: topic://ActiveMQ.Advisory.Connection");
        }
    }

    @Test
    public void testCreateQueuePublishConsume() throws Exception {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost");
        try {
            Connection conn = factory.createQueueConnection("admin", "sunflower");
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            conn.start();
            Queue queue = sess.createQueue("TEST.FOO");

            MessageProducer producer = sess.createProducer(queue);
            MessageConsumer consumer = sess.createConsumer(queue);

            producer.send(sess.createTextMessage("test"));
            Message msg = consumer.receive(1000);
            assertNotNull(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
