package com.vaigay.reactive;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

@Slf4j
@Path("/matches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MatchController {

	@Inject
	MatchRepository matchRepository;

	@POST()
	@Path("/add")
	public Uni<Match> addMatchApi(Match match) {
		return Uni.createFrom().publisher(addMatch(match));
	}

	private Mono<Match> addMatch(Match match) {
		log.info("123");
		match.setId(UUID.randomUUID().toString());
		return matchRepository.findMatchById(match.getId()).flatMap(match1 -> {
			log.info("Found");
			return Mono.error(new Throwable("Match existed"));
		}).switchIfEmpty(matchRepository.addMatch(match)).then(getMatchById(match.getId()));
	}

	@GET
	@Path("/get/{matchId}")
	public Uni<Match> getMatchByIdApi(@PathParam("matchId") String matchId) {
		return Uni.createFrom().publisher(getMatchById(matchId));
	}

	private Mono<Match> getMatchById(String matchId) {
		return matchRepository.findMatchById(matchId);
	}

	@POST()
	@Path("/ticket/changeStatus/{matchId}/{ticketId}")
	public Uni<String> changeTicketStatusApi(@PathParam("matchId") String matchId,
			@PathParam("ticketId") String ticketId) {

		return Uni.createFrom().publisher(changeTicketStatus(matchId, ticketId));
	}

	private Mono<String> changeTicketStatus(String matchId, String ticketId) {
		if (matchRepository.findMatchExitsById(matchId))
			return matchRepository.changeTicketStatus(matchId, ticketId);
		return Mono.just("No match found");
	}

	@GET
	@Path("/get/available/{matchId}")
	public Multi<Ticket> getAvailableTicketApi(@PathParam("matchId") String matchId) {
		return Multi.createFrom().publisher(getAvailableTicket(matchId));
	}

	private Flux<Ticket> getAvailableTicket(String matchId) {
		return matchRepository.findAvailableTicket(matchId);
	}

}
