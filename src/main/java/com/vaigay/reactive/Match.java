package com.vaigay.reactive;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Match {
    private String id;
    private int numberOfTicket;
    private List<Ticket> ticketList;

}
