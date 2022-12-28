package com.joshlong.spring.blogs.spring.team;

import com.joshlong.spring.blogs.TeamEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * the idea is that the team dynamic is a Spring-specific thing.
 * Let's move it out of the main path of promoting a blog. We'll keep the business logic here
 * and introduce some sort of adapter SPI to the main path so that any interested
 * contributors can enrich the data for a given blog post by, among other things, assigning them
 * social media and so on. This information can be presented in some sort of context available
 * when we template the text.
 */
@Service
@RequiredArgsConstructor
class TeamService {

    private final JdbcTemplate ds;

    private final TransactionTemplate tx;

    @EventListener
    public void teammate(TeamEvent teamEvent) {
        var teammates = teamEvent.teammates();
        tx.execute(status -> {
            ds.execute("update spring_teammates set fresh = false");
            teammates.forEach(teammate -> {
                var sql = """

                        insert into spring_teammates (
                            url,
                            name ,
                            position,
                            location,
                            github,
                            twitter ,
                            fresh
                        )
                        values ( ?, ?, ?, ?, ?, ?, ? )
                        on conflict  ( name) do update set
                            url = excluded.url,
                            position = excluded.position,
                            location = excluded.location,
                            github = excluded.github,
                            twitter = excluded.twitter ,
                            fresh = excluded.fresh
                        """;
                ds.update(sql, ps -> {
                    ps.setString(1, teammate.page().toString());
                    ps.setString(2, teammate.name());
                    ps.setString(3, teammate.position());
                    ps.setString(4, teammate.location());
                    ps.setString(5, teammate.github());
                    ps.setString(6, teammate.twitter());
                    ps.setBoolean(7, true);
                    ps.execute();
                });
            });

            return null;
        });
    }
}
