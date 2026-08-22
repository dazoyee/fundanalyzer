select count(*)
from (
    select
        id,
        event_type
    from
        system_event
    order by
        occurred_at desc,
        id desc
    limit
        /* limit */20
) recent_events
where
    event_type = /* eventType */'ERROR'
