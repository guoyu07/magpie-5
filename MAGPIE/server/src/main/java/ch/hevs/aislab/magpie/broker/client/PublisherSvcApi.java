package ch.hevs.aislab.magpie.broker.client;

import javax.servlet.http.HttpServletResponse;

import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;
import ch.hevs.aislab.magpie.broker.model.SubscriptionResult;

public interface PublisherSvcApi {

	String PUBLISHER_ID = "pubId";
	
	String PUB_SVC = "/publisher";
	String PUB_CONFIRM_SVC = PUB_SVC + "/{" + PUBLISHER_ID + "}/confirmSubscription";
	
	
	@POST(PUB_CONFIRM_SVC)
	boolean confirmSubscription(
			@Path(PUBLISHER_ID) long pubId,
			@Body SubscriptionResult subscriptionResult,
			HttpServletResponse response);
	
}