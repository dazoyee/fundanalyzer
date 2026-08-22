select count(*)
from (
    select
        id,
        event_type
    from
        system_event
    where
        occurred_at >= /* occurredAtSince */'2026-01-01 00:00:00'
    order by
        occurred_at desc,
        id desc
    limit
        /* limit */100
) recent_events
where
    event_type = /* eventType */'ERROR'
