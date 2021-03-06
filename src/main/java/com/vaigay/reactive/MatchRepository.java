package com.vaigay.reactive;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@ApplicationScoped

public class MatchRepository {

	private final MongoCollection<Match> collection;

	public MatchRepository() {
		CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				fromProviders(PojoCodecProvider.builder().automatic(true).build()));
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder().applyConnectionString(connectionString)
				.retryWrites(true).codecRegistry(codecRegistry).build();
		MongoClient mongoClient = MongoClients.create(mongoClientSettings);
		MongoDatabase mongoDatabase = mongoClient.getDatabase("MatchDb");
		collection = mongoDatabase.getCollection("Match", Match.class);
	}

	public Mono<Match> addMatch(Match match) {
		return Mono.from(collection.insertOne(match)).then(Mono.just(match));
	}

	public Mono<Match> findMatchById(String matchId) {
		return Mono.from(collection.find(Filters.eq("_id", matchId)).first());
	}

	public Mono<String> changeTicketStatus(String matchId, String ticketId) {
		List<Bson> filters = new ArrayList<>();
		Bson filter1 = Filters.eq("_id", matchId);
		Bson filter2 = Filters.eq("ticketList._id", ticketId);
		filters.add(filter1);
		filters.add(filter2);
		Bson update = Updates.set("ticketList.$.status", false);
		collection.updateOne(Filters.and(filters), update);
		return Mono.from(collection.updateOne(Filters.and(filters), update))
				.flatMap(updateResult -> checkResult(updateResult, ticketId, matchId));
	}

	private Mono<String> checkResult(UpdateResult upResult, String tiketId, String matchId) {
		String res;
		System.out.println(upResult.getModifiedCount());
		if (upResult.getModifiedCount() > 0)
			res = new String("Booked Successfully");
		else
			res = "Couldn't book ticket:" + tiketId + "\nOf match: " + matchId;
		return Mono.just(res);
	}

	public Flux<Ticket> findAvailableTicket(String matchId) {
		Bson filter = Filters.eq("_id", matchId);
		return Flux.from(collection.find(filter).first()).flatMap(
				match -> Flux.fromIterable(match.getTicketList()).filter(ticket -> ticket.getStatus().equals(true)));
	}

	public boolean findMatchExitsById(String matchId) {
		return findMatchById(matchId).hasElement().block();
	}

}
