select
    /*%expand*/*
from
    system_event
order by
    occurred_at desc,
    id desc
limit
    /* limit */20
