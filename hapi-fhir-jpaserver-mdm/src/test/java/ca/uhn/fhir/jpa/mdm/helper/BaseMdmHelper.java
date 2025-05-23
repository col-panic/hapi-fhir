package ca.uhn.fhir.jpa.mdm.helper;

import ca.uhn.fhir.broker.jms.SpringMessagingReceiverAdapter;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.mdm.broker.MdmQueueConsumerLoader;
import ca.uhn.fhir.jpa.mdm.config.MdmSubscriptionLoader;
import ca.uhn.fhir.jpa.subscription.channel.impl.LinkedBlockingChannel;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionLoader;
import ca.uhn.fhir.jpa.subscription.match.registry.SubscriptionRegistry;
import ca.uhn.fhir.jpa.subscription.model.ResourceModifiedMessage;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.test.concurrency.PointcutLatch;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Supplier;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * How to use this Rule:
 * <p>
 * This rule is to be used whenever you want to have the MdmInterceptor loaded, and be able
 * to execute creates/updates/deletes while being assured that all MDM work has been done before exiting.
 * Provides two types of method:
 * <p>
 * 1. doUpdate/doCreate. These methods do not wait for Asynchronous MDM work to be done. Use these when you are expecting
 * the calls to fail, as those hooks will never be called.
 * <p>
 * 2. createWithLatch/updateWithLatch. These methods will await the MDM hooks, which are only triggered post-MDM processing
 * You should use these when you are expecting successful processing of the resource, and need to wait for async MDM linking
 * work to be done.
 * <p>
 * Note: all create/update functions take an optional isExternalHttpRequest boolean, to make it appear as though the request's
 * origin is an HTTP request.
 */
public abstract class BaseMdmHelper implements BeforeEachCallback, AfterEachCallback {
	@Mock
	protected ServletRequestDetails myMockSrd;
	@Mock
	protected HttpServletRequest myMockServletRequest;
	@Mock
	protected RestfulServer myMockRestfulServer;
	@Mock
	protected FhirContext myMockFhirContext;
	protected PointcutLatch myAfterMdmLatch = new PointcutLatch(Pointcut.MDM_AFTER_PERSISTED_RESOURCE_CHECKED);
	@Autowired
	MdmQueueConsumerLoader myMdmQueueConsumerLoader;
	@Autowired
	SubscriptionRegistry mySubscriptionRegistry;
	@Autowired
	SubscriptionLoader mySubscriptionLoader;
	@Autowired
	MdmSubscriptionLoader myMdmSubscriptionLoader;
	@Mock
	private IInterceptorBroadcaster myMockInterceptorBroadcaster;
	@Autowired
	private IInterceptorService myInterceptorService;

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		myInterceptorService.unregisterInterceptor(myAfterMdmLatch);
		myAfterMdmLatch.clear();
		waitUntilMdmQueueIsEmpty();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		//This sets up mock servlet request details, which allows our DAO requests to appear as though
		//they are coming from an external HTTP Request.
		MockitoAnnotations.initMocks(this);
		when(myMockSrd.getInterceptorBroadcaster()).thenReturn(myMockInterceptorBroadcaster);
		when(myMockInterceptorBroadcaster.callHooks(any(Pointcut.class), any(HookParams.class))).thenReturn(true);
		when(myMockSrd.getServletRequest()).thenReturn(myMockServletRequest);
		when(myMockSrd.getServer()).thenReturn(myMockRestfulServer);
		when(myMockSrd.getRequestId()).thenReturn("MOCK_REQUEST");
		when(myMockRestfulServer.getFhirContext()).thenReturn(myMockFhirContext);

		//This sets up our basic interceptor, and also attached the latch so we can await the hook calls.
		myInterceptorService.registerAnonymousInterceptor(Pointcut.MDM_AFTER_PERSISTED_RESOURCE_CHECKED, myAfterMdmLatch);

		// We need to call this because subscriptions will get deleted in @After cleanup
		waitForActivatedSubscriptionCount(0);
		myMdmSubscriptionLoader.daoUpdateMdmSubscriptions();
		waitForActivatedSubscriptionCount(2);
	}


	protected void waitForActivatedSubscriptionCount(int theSize) {
		await("Active Subscription Count has reached " + theSize).until(() -> mySubscriptionRegistry.size() >= theSize);
	}

	private void waitUntilMdmQueueIsEmpty() {
		await().until(() -> getExecutorQueueSize() == 0);
	}

	public int getExecutorQueueSize() {
		SpringMessagingReceiverAdapter<ResourceModifiedMessage> adapter = (SpringMessagingReceiverAdapter<ResourceModifiedMessage>) myMdmQueueConsumerLoader.getMdmChannelConsumerForUnitTest();
		LinkedBlockingChannel channel = (LinkedBlockingChannel) adapter.getSpringMessagingChannelReceiver();
		return channel.getQueueSizeForUnitTest();
	}

	public PointcutLatch getAfterMdmLatch() {
		return myAfterMdmLatch;
	}

	/**
	 * Expect 1 call to the MDM_AFTER_PERSISTED_RESOURCE_CHECKED pointcut when calling theSupplier.  Wait until
	 * the mdm message arrives and this pointcut is called before returning the result of theSupplier.
	 * @param theSupplier
	 * @return
	 * @param <T>
	 * @throws InterruptedException
	 */
	public <T> T executeWithLatch(Supplier<T> theSupplier) throws InterruptedException {
		myAfterMdmLatch.setExpectedCount(1);
		T retval = theSupplier.get();
		myAfterMdmLatch.awaitExpected();
		return retval;
	}
}
