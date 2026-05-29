# proyecto_syncro
Plataforma de logistica de inventario




Syncro es una plataforma de gestión logística diseñada para integrarse en cualquier PYME o empresa que requiera sincronización en tiempo real de su inventario, pedidos y envíos, independientemente del rubro al que pertenezca. Su objetivo central es eliminar la fragmentación de datos propia de los sistemas monolíticos o manuales, permitiendo que múltiples procesos (recepción de pedidos, actualización de stock y coordinación de despachos) operen de manera simultánea y desacoplada.
El problema que Syncro resuelve es crítico para las PYMEs modernas: los sistemas actuales no permiten que módulos independientes escalen de forma autónoma, y una falla en un componente puede derribar toda la operación. Syncro propone una solución basada en microservicios y arquitectura orientada a eventos (EDA), donde cada módulo (inventario, pedidos y envíos) opera en forma independiente, comunicándose de manera asíncrona mediante un message broker.



1.1 Objetivos Específicos del Sistema

Pedidos: Gestionar el ciclo de vida completo de un pedido, desde su creación hasta su despacho y entrega.
Inventario: Mantener el inventario sincronizado en tiempo real al procesar cada orden de venta.
Envíos: Coordinar los envíos y proveer trazabilidad del estado de cada despacho
Resiliencia: Garantizar que una falla en cualquier microservicio no afecte la operación de los demás.
Escalabilidad: Proveer una arquitectura escalable horizontalmente que soporte crecimiento sin rediseño.

